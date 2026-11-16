from django.apps import AppConfig
from django.template.context import BaseContext


def _py314_safe_base_context_copy(self):
    """
    Backports Django's Python 3.14 compatibility fix (Django Ticket #35844)
    where `copy(super())` in Django 5.1's `BaseContext.__copy__` returns a `super`
    proxy object instead of a copied Context instance when rendering Admin templates.
    """
    duplicate = BaseContext.__new__(self.__class__)
    duplicate.__dict__ = self.__dict__.copy()
    duplicate.dicts = self.dicts[:]
    return duplicate


BaseContext.__copy__ = _py314_safe_base_context_copy


class MewLogisticsConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'mew_logistics'

    def ready(self):
        import mew_logistics.signals  # noqa: F401

