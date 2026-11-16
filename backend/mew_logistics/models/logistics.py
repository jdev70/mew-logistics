from django.db import models
from django.utils.translation import gettext_lazy as _
from .base import BaseModel, ReadinessLevel
from .organization import Installation, MilitaryUnit
from .personnel import Personnel
from .assets import Asset, MaterialCatalog

class WorkOrder(BaseModel):
    asset = models.ForeignKey(Asset, on_delete=models.CASCADE, related_name='work_orders')
    technician = models.ForeignKey(Personnel, on_delete=models.SET_NULL, null=True, related_name='assigned_work')
    description_of_fault = models.TextField()
    date_opened = models.DateTimeField(auto_now_add=True)
    date_closed = models.DateTimeField(null=True, blank=True)
    is_grounding_fault = models.BooleanField(default=False)
    status = models.CharField(max_length=50, default='Open')

    def __str__(self):
        return f"WO-{str(self.id)[:8].upper()} ({self.asset.serial_number}) - {self.status}"

class Requisition(BaseModel):
    class UrgencyPriority(models.IntegerChoices):
        ROUTINE = 3, _('Routine (03)')
        PRIORITY = 2, _('Priority (02)')
        URGENT = 1, _('Urgent (01)')

    document_number = models.CharField(max_length=25, unique=True)
    requesting_unit = models.ForeignKey(MilitaryUnit, on_delete=models.CASCADE, related_name='requisitions')
    material = models.ForeignKey(MaterialCatalog, on_delete=models.PROTECT, related_name='requisitions')
    quantity = models.PositiveIntegerField()
    priority = models.IntegerField(choices=UrgencyPriority.choices, default=UrgencyPriority.ROUTINE)
    tied_to_asset = models.ForeignKey(Asset, on_delete=models.SET_NULL, null=True, blank=True)
    work_order = models.ForeignKey(
        WorkOrder,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='requisitions',
    )
    status = models.CharField(max_length=50, default='Submitted')
    estimated_delivery_date = models.DateField(null=True, blank=True)

    def __str__(self):
        return f"REQ {self.document_number} - {self.material.nsn}"

class InventoryBalance(BaseModel):
    installation = models.ForeignKey(Installation, on_delete=models.CASCADE, related_name='inventory')
    material = models.ForeignKey(MaterialCatalog, on_delete=models.CASCADE, related_name='balances')
    quantity_on_hand = models.PositiveIntegerField(default=0)
    reorder_point = models.PositiveIntegerField(default=5)
    safety_stock = models.PositiveIntegerField(default=2)

    class Meta:
        unique_together = ('installation', 'material')

    def needs_replenishment(self):
        return self.quantity_on_hand <= self.reorder_point

class WorkOrderPart(BaseModel):
    work_order = models.ForeignKey(WorkOrder, on_delete=models.CASCADE, related_name='parts_used')
    material = models.ForeignKey(MaterialCatalog, on_delete=models.PROTECT)
    quantity = models.PositiveIntegerField(default=1)

    def __str__(self):
        return f"{self.quantity}x {self.material.nomenclature} for WO {self.work_order.id}"