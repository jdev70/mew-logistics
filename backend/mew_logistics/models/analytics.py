from django.db import models, transaction
from django.core.exceptions import ValidationError
from .base import BaseModel, ReadinessLevel, TransactionType
from .organization import MilitaryUnit
from .assets import Asset
from .personnel import Personnel
from .logistics import Requisition, WorkOrder, InventoryBalance

class UnitReadinessSnapshot(models.Model):
    id = models.BigAutoField(primary_key=True)
    unit = models.ForeignKey(MilitaryUnit, on_delete=models.CASCADE)
    snapshot_date = models.DateField(auto_now_add=True)
    
    total_personnel = models.IntegerField()
    deployable_personnel = models.IntegerField()
    total_assets = models.IntegerField()
    fmc_assets = models.IntegerField()
    pmc_assets = models.IntegerField()
    nmc_assets = models.IntegerField()

    class Meta:
        unique_together = ('unit', 'snapshot_date')
        indexes = [models.Index(fields=['snapshot_date', 'unit'])]

    @property
    def operational_readiness_rate(self):
        if self.total_assets == 0:
            return 0
        return ((self.fmc_assets + self.pmc_assets) / self.total_assets) * 100

class AssetStatusLog(BaseModel):
    asset = models.ForeignKey(Asset, on_delete=models.CASCADE, related_name='status_history')
    old_status = models.CharField(max_length=4, choices=ReadinessLevel.choices, null=True, blank=True)
    new_status = models.CharField(max_length=4, choices=ReadinessLevel.choices)
    
    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['asset', '-created_at']),
            models.Index(fields=['new_status']),
        ]

    def __str__(self):
        return f"{self.asset.serial_number}: {self.old_status or 'NEW'} -> {self.new_status}"

class InventoryTransaction(BaseModel):
    balance = models.ForeignKey(InventoryBalance, on_delete=models.PROTECT, related_name='transactions')
    transaction_type = models.CharField(max_length=3, choices=TransactionType.choices)
    quantity = models.PositiveIntegerField()
    previous_balance = models.PositiveIntegerField(editable=False)
    new_balance = models.PositiveIntegerField(editable=False)
    
    reference_document = models.CharField(max_length=100, blank=True, null=True)
    related_requisition = models.ForeignKey(Requisition, on_delete=models.SET_NULL, null=True, blank=True)
    related_work_order = models.ForeignKey(WorkOrder, on_delete=models.SET_NULL, null=True, blank=True)
    performed_by = models.ForeignKey(Personnel, on_delete=models.SET_NULL, null=True, blank=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['balance', '-created_at']),
            models.Index(fields=['transaction_type']),
            models.Index(fields=['reference_document']),
        ]

    def __str__(self):
        sign = "+" if self.transaction_type == TransactionType.RECEIPT else "-"
        return f"{self.balance.material.nsn}: {sign}{self.quantity} ({self.created_at.date()})"

    def save(self, *args, **kwargs):
        """Prevents modification of existing ledger entries."""
        # Use _state.adding instead of checking self.pk because UUIDs are pre-populated
        if not self._state.adding:
            raise ValidationError("Inventory transactions are immutable ledgers and cannot be modified.")
        super().save(*args, **kwargs)

    @classmethod
    def execute(cls, balance_id, transaction_type, quantity, performed_by=None, ref_doc=None, req=None, wo=None):
        with transaction.atomic():
            balance = InventoryBalance.objects.select_for_update().get(id=balance_id)
            prev_qty = balance.quantity_on_hand
            
            if transaction_type == TransactionType.RECEIPT:
                new_qty = prev_qty + quantity
            elif transaction_type == TransactionType.DEDUCTION:
                if prev_qty < quantity:
                    raise ValidationError(f"Insufficient stock. Cannot deduct {quantity} from {prev_qty}.")
                new_qty = prev_qty - quantity
            else: 
                new_qty = quantity
                quantity = abs(new_qty - prev_qty)
            
            balance.quantity_on_hand = new_qty
            balance.save(update_fields=['quantity_on_hand'])
            
            return cls.objects.create(
                balance=balance,
                transaction_type=transaction_type,
                quantity=quantity,
                previous_balance=prev_qty,
                new_balance=new_qty,
                reference_document=ref_doc,
                related_requisition=req,
                related_work_order=wo,
                performed_by=performed_by
            )