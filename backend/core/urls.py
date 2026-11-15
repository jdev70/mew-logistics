from django.contrib import admin
from django.urls import path, include

urlpatterns = [
    # The default Django admin panel
    path('admin/', admin.site.urls),
    
    # Route all SvcV-1 API requests to the mew_logistics application
    path('api/', include('mew_logistics.urls')),
]
