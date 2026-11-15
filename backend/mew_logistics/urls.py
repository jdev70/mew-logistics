from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    MilitaryUnitDashboardViewSet,
    AssetViewSet,
    RequisitionViewSet,
    WorkOrderViewSet,
    InventoryTransactionViewSet,
)

# Initialize the DefaultRouter which provides standard REST routing
router = DefaultRouter()

# Register the endpoints
router.register(r'dashboard/units', MilitaryUnitDashboardViewSet, basename='unit-dashboard')
router.register(r'assets', AssetViewSet, basename='asset')
router.register(r'requisitions', RequisitionViewSet, basename='requisition')
router.register(r'work-orders', WorkOrderViewSet, basename='work-order')
router.register(r'inventory-transactions', InventoryTransactionViewSet, basename='inventory-transaction')

urlpatterns = [
    # Include the generated routes under the API v1 namespace
    path('api/v1/', include(router.urls)),
]