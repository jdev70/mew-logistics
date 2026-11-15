from datetime import timedelta
from celery import shared_task
from django.db.models import Prefetch
from django.db.models import Count, Q
from django.utils import timezone
from .models import Asset, AssetStatusLog, MilitaryUnit, ReadinessLevel, UnitReadinessSnapshot

@shared_task
def generate_daily_readiness_snapshots():
    """
    Calculates daily readiness metrics using a single database query, 
    then processes thousands of records in memory and saves them in bulk.
    """
    today = timezone.now().date()
    
    # 1. Execute a single query to aggregate all metrics across all units
    units_with_metrics = MilitaryUnit.objects.annotate(
        total_personnel_count=Count('personnel', distinct=True),
        deployable_personnel_count=Count(
            'personnel',
            filter=Q(personnel__is_medically_deployable=True, personnel__is_training_current=True),
            distinct=True
        ),
        total_assets_count=Count('assets', distinct=True),
        fmc_assets_count=Count('assets', filter=Q(assets__readiness_status='FMC'), distinct=True),
        pmc_assets_count=Count('assets', filter=Q(assets__readiness_status='PMC'), distinct=True),
        nmc_assets_count=Count('assets', filter=Q(assets__readiness_status__in=['NMCM', 'NMCS', 'DLM']), distinct=True)
    )

    # 2. Fetch today's existing snapshots into memory to determine if we update or create
    # This prevents throwing UniqueConstraint errors if the task runs twice in one day
    existing_snapshots = {
        snapshot.unit_id: snapshot 
        for snapshot in UnitReadinessSnapshot.objects.filter(snapshot_date=today)
    }

    # 3. Prepare memory arrays for bulk operations
    snapshots_to_create = []
    snapshots_to_update = []
    units_to_update = []

    for unit in units_with_metrics:
        # Update the parent Unit's rolling percentages
        if unit.total_assets_count > 0:
            unit.equipment_readiness_pct = ((unit.fmc_assets_count + unit.pmc_assets_count) / unit.total_assets_count) * 100
        
        if unit.total_personnel_count > 0:
            unit.personnel_readiness_pct = (unit.deployable_personnel_count / unit.total_personnel_count) * 100
            
        units_to_update.append(unit)

        # Map the aggregated data to a Snapshot instance
        if unit.id in existing_snapshots:
            # Update existing snapshot in memory
            snapshot = existing_snapshots[unit.id]
            snapshot.total_personnel = unit.total_personnel_count
            snapshot.deployable_personnel = unit.deployable_personnel_count
            snapshot.total_assets = unit.total_assets_count
            snapshot.fmc_assets = unit.fmc_assets_count
            snapshot.pmc_assets = unit.pmc_assets_count
            snapshot.nmc_assets = unit.nmc_assets_count
            
            snapshots_to_update.append(snapshot)
        else:
            # Stage a new snapshot for creation
            snapshots_to_create.append(
                UnitReadinessSnapshot(
                    unit=unit,
                    snapshot_date=today,
                    total_personnel=unit.total_personnel_count,
                    deployable_personnel=unit.deployable_personnel_count,
                    total_assets=unit.total_assets_count,
                    fmc_assets=unit.fmc_assets_count,
                    pmc_assets=unit.pmc_assets_count,
                    nmc_assets=unit.nmc_assets_count,
                )
            )

    # 4. Commit all staged changes to the database using batch_size to respect SQL limits
    BATCH_SIZE = 1000

    if snapshots_to_create:
        UnitReadinessSnapshot.objects.bulk_create(snapshots_to_create, batch_size=BATCH_SIZE)

    if snapshots_to_update:
        UnitReadinessSnapshot.objects.bulk_update(
            snapshots_to_update,
            fields=[
                'total_personnel', 'deployable_personnel', 
                'total_assets', 'fmc_assets', 'pmc_assets', 'nmc_assets'
            ],
            batch_size=BATCH_SIZE
        )

    if units_to_update:
        MilitaryUnit.objects.bulk_update(
            units_to_update,
            fields=['equipment_readiness_pct', 'personnel_readiness_pct'],
            batch_size=BATCH_SIZE
        )

    return f"Success: Created {len(snapshots_to_create)} and updated {len(snapshots_to_update)} snapshots. Updated {len(units_to_update)} units."

@shared_task
def calculate_fleet_reliability_metrics():
    """
    Asynchronously calculates MTBF, MTTR, and Ao for all assets 
    and updates the database in bulk.
    """
    now = timezone.now()
    down_statuses = [
        ReadinessLevel.NON_MISSION_CAPABLE_MAINTENANCE,
        ReadinessLevel.NON_MISSION_CAPABLE_SUPPLY,
        ReadinessLevel.DEPOT_LEVEL_MAINTENANCE
    ]

    # Prefetch logs to prevent N+1 queries during the loop
    assets = Asset.objects.prefetch_related(
        Prefetch(
            'status_history', 
            queryset=AssetStatusLog.objects.order_by('created_at'),
            to_attr='prefetched_logs'
        )
    )

    assets_to_update = []

    for asset in assets:
        total_uptime = timedelta(0)
        total_downtime = timedelta(0)
        failure_count = 0
        repair_count = 0
        
        last_up_time = None
        down_start_time = None

        # Process all history in a single O(N) pass
        for log in asset.prefetched_logs:
            if log.new_status in down_statuses:
                # Asset went DOWN: Stop uptime clock, start downtime clock
                if last_up_time is not None:
                    total_uptime += (log.created_at - last_up_time)
                    failure_count += 1
                    last_up_time = None
                    
                if down_start_time is None:
                    down_start_time = log.created_at
            else:
                # Asset went UP: Stop downtime clock, start uptime clock
                if down_start_time is not None:
                    total_downtime += (log.created_at - down_start_time)
                    repair_count += 1
                    down_start_time = None
                    
                if last_up_time is None:
                    last_up_time = log.created_at

        # Factor in ongoing current state
        if last_up_time is not None:
            total_uptime += (now - last_up_time)
        if down_start_time is not None:
            total_downtime += (now - down_start_time)
            repair_count += 1

        # Calculate final metrics in hours
        mtbf = (total_uptime / failure_count).total_seconds() / 3600 if failure_count > 0 else total_uptime.total_seconds() / 3600
        mttr = (total_downtime / repair_count).total_seconds() / 3600 if repair_count > 0 else 0.0
        
        # Calculate Operational Availability (Ao)
        total_cycle_time = mtbf + mttr
        ao = (mtbf / total_cycle_time * 100) if total_cycle_time > 0 else 100.0

        # Update instance values
        asset.mtbf_hours = round(mtbf, 2)
        asset.mttr_hours = round(mttr, 2)
        asset.operational_availability_pct = round(ao, 2)
        
        assets_to_update.append(asset)

    # Commit all changes to the database in efficient batches
    if assets_to_update:
        Asset.objects.bulk_update(
            assets_to_update, 
            fields=['mtbf_hours', 'mttr_hours', 'operational_availability_pct'],
            batch_size=1000
        )

    return f"Successfully updated reliability metrics for {len(assets_to_update)} assets."