import uuid
from django.db import models
from django.utils.translation import gettext_lazy as _

class BaseModel(models.Model):
    """Abstract base model with common enterprise fields."""
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)
    updated_at = models.DateTimeField(auto_now=True)
    is_active = models.BooleanField(default=True)
    remarks = models.TextField(blank=True, null=True)

    class Meta:
        abstract = True

class ReadinessLevel(models.TextChoices):
    FULLY_MISSION_CAPABLE = 'FMC', _('Fully Mission Capable')
    PARTIALLY_MISSION_CAPABLE = 'PMC', _('Partially Mission Capable')
    NON_MISSION_CAPABLE_MAINTENANCE = 'NMCM', _('Non-Mission Capable (Maintenance)')
    NON_MISSION_CAPABLE_SUPPLY = 'NMCS', _('Non-Mission Capable (Supply)')
    DEPOT_LEVEL_MAINTENANCE = 'DLM', _('Depot Level Maintenance')

class TransactionType(models.TextChoices):
    RECEIPT = 'RCT', 'Receipt (+)'
    DEDUCTION = 'DED', 'Deduction (-)'
    ADJUSTMENT = 'ADJ', 'Adjustment (+/-)'