package mil.dod.mew.web.controller;

import mil.dod.mew.service.CatalogSourceReconciliationService;
import mil.dod.mew.service.RbacPolicyService;
import mil.dod.mew.service.RbacPolicyService.Action;
import mil.dod.mew.service.RbacPolicyService.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrative endpoint for reconciling the packaged, reviewed source manifest. */
@RestController
@RequestMapping("/api/v1/catalog-sources")
public class CatalogSourceController {
    private final CatalogSourceReconciliationService reconciliation;
    private final RbacPolicyService rbac;

    public CatalogSourceController(CatalogSourceReconciliationService reconciliation, RbacPolicyService rbac) {
        this.reconciliation = reconciliation;
        this.rbac = rbac;
    }

    @PostMapping("/reconcile")
    public CatalogSourceReconciliationService.ReconciliationResult reconcile(
            @RequestParam(defaultValue = "false") boolean apply) {
        rbac.requirePermission(Resource.CATALOG, Action.UPDATE, "/api/v1/catalog-sources/reconcile");
        return apply ? reconciliation.applyBundledManifest() : reconciliation.previewBundledManifest();
    }
}
