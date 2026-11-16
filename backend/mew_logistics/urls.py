from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    AssetViewSet,
    HullSystemAssignmentViewSet,
    InstallationViewSet,
    InventoryBalanceViewSet,
    InventoryTransactionViewSet,
    MaterialCatalogViewSet,
    MilitaryUnitDashboardViewSet,
    NavalHullViewSet,
    PersonnelViewSet,
    RequisitionViewSet,
    WorkOrderViewSet,
)

# Initialize the DefaultRouter which provides standard REST routing
router = DefaultRouter()

# Register the SvcV-1 REST endpoints
router.register(r'dashboard/units', MilitaryUnitDashboardViewSet, basename='unit-dashboard')
router.register(r'assets', AssetViewSet, basename='asset')
router.register(r'installations', InstallationViewSet, basename='installation')
router.register(r'personnel', PersonnelViewSet, basename='personnel')
router.register(r'hulls', NavalHullViewSet, basename='hull')
router.register(r'loadouts', HullSystemAssignmentViewSet, basename='loadout')
router.register(r'components', MaterialCatalogViewSet, basename='component')
router.register(r'inventory-balances', InventoryBalanceViewSet, basename='inventory-balance')
router.register(r'requisitions', RequisitionViewSet, basename='requisition')
router.register(r'work-orders', WorkOrderViewSet, basename='work-order')
router.register(r'inventory-transactions', InventoryTransactionViewSet, basename='inventory-transaction')

urlpatterns = [
    # Primary SvcV-1 namespace (/api/v1/...)
    path('v1/', include(router.urls)),
    # Backwards-compatible alias (/api/mew-assets/ -> /api/v1/assets/)
    path('', include(router.urls)),
]
