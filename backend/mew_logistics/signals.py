from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models.signals import post_save, post_delete, pre_save
from django.dispatch import receiver
from .models import AssetStatusLog, WorkOrder, Requisition, Asset, ReadinessLevel, InventoryTransaction, TransactionType, InventoryBalance

def evaluate_asset_readiness(asset):
    """
    Recalculates and updates the readiness status of an asset based on 
    its current active work orders and pending requisitions.
    """
    if not asset:
        return

    # 1. Check for grounding faults (deadlining issues)
    active_grounding_faults = asset.work_orders.filter(
        is_grounding_fault=True
    ).exclude(status='Closed')

    if active_grounding_faults.exists():
        # 2. Check if we are waiting on parts for this deadlined asset
        pending_requisitions = asset.requisition_set.exclude(status='Received')
        
        if pending_requisitions.exists():
            new_status = ReadinessLevel.NON_MISSION_CAPABLE_SUPPLY  # NMCS
        else:
            new_status = ReadinessLevel.NON_MISSION_CAPABLE_MAINTENANCE  # NMCM
            
    else:
        # 3. No grounding faults. Check for minor, non-grounding faults.
        active_minor_faults = asset.work_orders.filter(
            is_grounding_fault=False
        ).exclude(status='Closed')
        
        if active_minor_faults.exists():
            new_status = ReadinessLevel.PARTIALLY_MISSION_CAPABLE  # PMC
        else:
            new_status = ReadinessLevel.FULLY_MISSION_CAPABLE  # FMC

    # Save only if the status has actually changed to prevent recursive signal loops
    if asset.readiness_status != new_status:
        asset.readiness_status = new_status
        asset.save(update_fields=['readiness_status', 'last_status_change'])

@receiver(pre_save, sender=Asset)
def capture_old_readiness_status(sender, instance, **kwargs):
    """
    Fires right before the Asset is saved to the database.
    Fetches the existing record to see what the status was before this update.
    """
    if instance.pk:
        try:
            # Query the database for the current state before the save overwrites it
            old_instance = sender.objects.get(pk=instance.pk)
            # Temporarily cache the old status on the instance object
            instance._old_readiness_status = old_instance.readiness_status
        except sender.DoesNotExist:
            instance._old_readiness_status = None
    else:
        # This is a brand new asset being created
        instance._old_readiness_status = None


@receiver(post_save, sender=Asset)
def log_readiness_status_change(sender, instance, created, **kwargs):
    """
    Fires immediately after the Asset is successfully saved to the database.
    Checks the cache and writes the audit log if a change occurred.
    """
    old_status = getattr(instance, '_old_readiness_status', None)
    
    # Check if the status actually changed (or if it's a brand new asset)
    if created or (old_status and old_status != instance.readiness_status):
        AssetStatusLog.objects.create(
            asset=instance,
            old_status=old_status,
            new_status=instance.readiness_status
        )
# ==========================================
# WORK ORDER SIGNALS
# ==========================================

@receiver(post_save, sender=WorkOrder)
def work_order_saved(sender, instance, **kwargs):
    """Triggered when a WorkOrder is created or updated."""
    evaluate_asset_readiness(instance.asset)


@receiver(post_delete, sender=WorkOrder)
def work_order_deleted(sender, instance, **kwargs):
    """Triggered if a WorkOrder is deleted from the system."""
    evaluate_asset_readiness(instance.asset)


# ==========================================
# REQUISITION SIGNALS
# ==========================================

@receiver(post_save, sender=Requisition)
def requisition_saved(sender, instance, **kwargs):
    """Triggered when a Requisition is created or updated."""
    if instance.tied_to_asset:
        evaluate_asset_readiness(instance.tied_to_asset)


@receiver(post_delete, sender=Requisition)
def requisition_deleted(sender, instance, **kwargs):
    """Triggered if a Requisition is deleted/cancelled."""
    if instance.tied_to_asset:
        evaluate_asset_readiness(instance.tied_to_asset)


@receiver(pre_save, sender=WorkOrder)
def cache_work_order_status(sender, instance, **kwargs):
    """
    Caches the old status before the database save so we can detect 
    if it is newly transitioning to 'Closed'.
    """
    if instance.pk:
        try:
            old_instance = sender.objects.get(pk=instance.pk)
            instance._old_status = old_instance.status
        except sender.DoesNotExist:
            instance._old_status = None
    else:
        instance._old_status = None


@receiver(post_save, sender=WorkOrder)
def deduct_inventory_on_close(sender, instance, created, **kwargs):
    """
    Executes an immutable deduction ledger entry for every part used 
    when a Work Order is officially closed.
    """
    old_status = getattr(instance, '_old_status', None)

    # Trigger only if the status just changed to 'Closed'
    if instance.status == 'Closed' and old_status != 'Closed':
        
        # Ensure the entire deduction process is atomic. 
        # If one part fails (e.g., negative stock), the whole Work Order closure rolls back.
        with transaction.atomic():
            for part in instance.parts_used.all():
                
                # Locate the inventory balance at the asset's current location
                try:
                    balance = InventoryBalance.objects.get(
                        installation=instance.asset.current_location,
                        material=part.material
                    )
                except InventoryBalance.DoesNotExist:
                    raise ValidationError(
                        f"Cannot close Work Order: {part.material.nsn} does not exist "
                        f"in the {instance.asset.current_location.name} warehouse."
                    )

                # Execute the safe ledger deduction
                InventoryTransaction.execute(
                    balance_id=balance.id,
                    transaction_type=TransactionType.DEDUCTION,
                    quantity=part.quantity,
                    performed_by=instance.technician,
                    ref_doc=f"WO-{instance.id}",
                    wo=instance
                )


@receiver(pre_save, sender=Requisition)
def cache_requisition_status(sender, instance, **kwargs):
    """
    Caches the old status before the database save so we can detect 
    if a shipment has just arrived and transitioned to 'Received'.
    """
    if instance.pk:
        try:
            old_instance = sender.objects.get(pk=instance.pk)
            instance._old_status = old_instance.status
        except sender.DoesNotExist:
            instance._old_status = None
    else:
        instance._old_status = None


@receiver(post_save, sender=Requisition)
def process_inventory_receipt(sender, instance, created, **kwargs):
    """
    Executes an immutable receipt ledger entry when a Requisition 
    status flips to 'Received'.
    """
    old_status = getattr(instance, '_old_status', None)

    if instance.status == 'Received' and old_status != 'Received':
        
        destination_installation = instance.requesting_unit.installation
        
        balance, _ = InventoryBalance.objects.get_or_create(
            installation=destination_installation,
            material=instance.material,
            defaults={
                'quantity_on_hand': 0,
                'reorder_point': 5,
                'safety_stock': 2
            }
        )

        # 1. Extract the injected personnel profile, defaulting to None if it wasn't passed
        # (e.g., if the requisition was updated via the Django Admin or a Celery task)
        performed_by_user = getattr(instance, '_received_by', None)

        # 2. Pass it to the ledger execution
        InventoryTransaction.execute(
            balance_id=balance.id,
            transaction_type=TransactionType.RECEIPT,
            quantity=instance.quantity,
            performed_by=performed_by_user,  # Injected here!
            ref_doc=f"REQ-{instance.document_number}",
            req=instance
        )