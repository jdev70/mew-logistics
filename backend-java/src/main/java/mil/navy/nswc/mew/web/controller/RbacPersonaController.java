package mil.navy.nswc.mew.web.controller;

import mil.navy.nswc.mew.service.RbacPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rbac/personas")
public class RbacPersonaController {

    private final RbacPolicyService rbacPolicyService;

    public RbacPersonaController(RbacPolicyService rbacPolicyService) {
        this.rbacPolicyService = rbacPolicyService;
    }

    @GetMapping({"", "/"})
    public Map<String, Object> getPersonas() {
        return rbacPolicyService.buildPersonasPayload(null);
    }
}
