from django.db import models
from django.db.models import Avg, FloatField, Q
from django.db.models.functions import Round

class MilitaryUnitQuerySet(models.QuerySet):
    def with_reliability_metrics(self, asset_class=None):
        """
        Annotates fleet-wide averages. If asset_class is provided, 
        it filters the averages to only include that specific type of equipment.
        """
        # Define the filter constraint for the aggregation
        if asset_class:
            # Traverses MilitaryUnit -> Asset -> AssetModel -> asset_class
            agg_filter = Q(assets__asset_model__asset_class=asset_class)
        else:
            agg_filter = None

        return self.annotate(
            fleet_avg_mtbf=Round(Avg(
                'assets__mtbf_hours', 
                filter=agg_filter, 
                output_field=FloatField()
            ), 2),
            
            fleet_avg_mttr=Round(Avg(
                'assets__mttr_hours', 
                filter=agg_filter, 
                output_field=FloatField()
            ), 2),
            
            fleet_avg_ao=Round(Avg(
                'assets__operational_availability_pct', 
                filter=agg_filter, 
                output_field=FloatField()
            ), 2)
        )

class MilitaryUnitManager(models.Manager):
    def get_queryset(self):
        return MilitaryUnitQuerySet(self.model, using=self._db)

    def with_reliability_metrics(self, asset_class=None):
        return self.get_queryset().with_reliability_metrics(asset_class=asset_class)