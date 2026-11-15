from django.db import models
from .base import BaseModel
from ..managers import MilitaryUnitManager # Assuming managers.py is in the app root

class Installation(BaseModel):
    name = models.CharField(max_length=255)
    dodaac = models.CharField(max_length=6, unique=True, help_text="DoD Activity Address Code")
    region = models.CharField(max_length=100)
    latitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)
    longitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True)

    def __str__(self):
        return f"{self.name} ({self.dodaac})"

class MilitaryUnit(BaseModel):
    name = models.CharField(max_length=255)
    uic = models.CharField(max_length=6, unique=True, help_text="Unit Identification Code")
    parent_unit = models.ForeignKey('self', on_delete=models.SET_NULL, null=True, blank=True, related_name='subordinate_units')

    objects = MilitaryUnitManager()
    installation = models.ForeignKey(Installation, on_delete=models.PROTECT, related_name='units')
    command_type = models.CharField(max_length=100)
    
    personnel_readiness_pct = models.DecimalField(max_digits=5, decimal_places=2, default=100.00)
    equipment_readiness_pct = models.DecimalField(max_digits=5, decimal_places=2, default=100.00)

    def __str__(self):
        return f"{self.name} [{self.uic}]"