from datetime import timedelta
from django.db import models
from django.utils import timezone
from .base import BaseModel, ReadinessLevel
from .organization import Installation, MilitaryUnit

class AssetModel(BaseModel):
    nomenclature = models.CharField(max_length=255)
    national_stock_number = models.CharField(max_length=16, unique=True)
    lin = models.CharField(max_length=10, help_text="Line Item Number", null=True, blank=True)
    asset_class = models.CharField(max_length=100)

    class Meta:
        indexes = [models.Index(fields=['asset_class'])]

    def __str__(self):
        return self.nomenclature

class MaterialCatalog(BaseModel):
    nsn = models.CharField(max_length=16, unique=True, verbose_name="National Stock Number")
    nomenclature = models.CharField(max_length=255)
    unit_of_issue = models.CharField(max_length=4)
    unit_price = models.DecimalField(max_digits=12, decimal_places=2)
    critical_item = models.BooleanField(default=False)

    def __str__(self):
        return f"{self.nsn} - {self.nomenclature}"

class Asset(BaseModel):
    asset_model = models.ForeignKey(AssetModel, on_delete=models.PROTECT, related_name='instances')
    serial_number = models.CharField(max_length=100, unique=True)
    assigned_unit = models.ForeignKey(MilitaryUnit, on_delete=models.CASCADE, related_name='assets')
    current_location = models.ForeignKey(Installation, on_delete=models.SET_NULL, null=True, related_name='housed_assets')
    
    readiness_status = models.CharField(max_length=4, choices=ReadinessLevel.choices, default=ReadinessLevel.FULLY_MISSION_CAPABLE)
    last_status_change = models.DateTimeField(auto_now_add=True)
    engine_hours = models.DecimalField(max_digits=10, decimal_places=2, default=0.0)
    
    mtbf_hours = models.FloatField(default=0.0, help_text="Mean Time Between Failures")
    mttr_hours = models.FloatField(default=0.0, help_text="Mean Time To Repair")
    operational_availability_pct = models.FloatField(default=100.0, help_text="Operational Availability (Ao)")

    class Meta:
        indexes = [
            models.Index(fields=['readiness_status', 'assigned_unit']),
            models.Index(fields=['operational_availability_pct']),
            models.Index(fields=['readiness_status', 'operational_availability_pct']),
        ]

    def __str__(self):
        return f"{self.asset_model.nomenclature} - SN: {self.serial_number}"

    @property
    def mtbf(self):
        return self.mtbf_hours

    @property
    def mttr(self):
        return self.mttr_hours

    @property
    def ao(self):
        return self.operational_availability_pct