from django.apps import AppConfig


class MewLogisticsConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'mew_logistics'

    def ready(self):
        import mew_logistics.signals  # noqa: F401
    
