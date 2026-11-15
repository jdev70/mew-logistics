from django.db import models
from django.contrib.auth.models import User
from .base import BaseModel
from .organization import MilitaryUnit

class Personnel(BaseModel):
    user = models.OneToOneField(User, on_delete=models.CASCADE, null=True, blank=True)
    first_name = models.CharField(max_length=100)
    middle_initial = models.CharField(max_length=1)
    last_name = models.CharField(max_length=100)
    unit = models.ForeignKey(MilitaryUnit, on_delete=models.CASCADE, related_name='personnel')
    rank = models.CharField(max_length=10)
    mos = models.CharField(max_length=10, help_text="Military Occupational Specialty code")
    
    is_medically_deployable = models.BooleanField(default=True)
    is_training_current = models.BooleanField(default=True)
    deployment_status = models.CharField(max_length=50, default="Garrison")
    
    def is_fully_deployable(self):
        return self.is_medically_deployable and self.is_training_current

    def __str__(self):
        return f"{self.rank} {self.first_name} {self.middle_initial}. {self.last_name} - {self.mos}"