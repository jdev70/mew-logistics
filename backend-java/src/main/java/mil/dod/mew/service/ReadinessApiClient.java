package mil.dod.mew.service;

import com.fasterxml.jackson.databind.JsonNode;
import mil.dod.mew.config.ReadinessApiProperties;
import mil.dod.mew.security.AuthenticatedMewUserResolver;
import mil.dod.mew.security.model.MewUser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Server-side client for the Django readiness API. Browser clients never receive its credential. */
@Service
public class ReadinessApiClient {
    private final ReadinessApiProperties properties;
    private final AuthenticatedMewUserResolver userResolver;
    private final RestTemplate restTemplate = new RestTemplate();

    public ReadinessApiClient(ReadinessApiProperties properties, AuthenticatedMewUserResolver userResolver) {
        this.properties = properties;
        this.userResolver = userResolver;
    }

    public ResponseEntity<JsonNode> get(String path, Map<String, String> query) {
        requireConfigured();
        MewUser user = userResolver.currentUser();
        String subject = StringUtils.hasText(properties.getLocalSubjectOverride())
                ? properties.getLocalSubjectOverride().trim() : user.subject();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gateway-Secret", properties.getGatewaySecret());
        headers.set("X-User-Id", subject);
        headers.set("X-User-Installation", user.assignedInstallationCode());

        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(properties.getBaseUrl()) + path);
        query.forEach((key, value) -> { if (StringUtils.hasText(value)) { uri.queryParam(key, value); } });
        return restTemplate.exchange(uri.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    public ResponseEntity<JsonNode> post(String path, Object payload) {
        requireConfigured();
        MewUser user = userResolver.currentUser();
        String subject = StringUtils.hasText(properties.getLocalSubjectOverride()) ? properties.getLocalSubjectOverride().trim() : user.subject();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gateway-Secret", properties.getGatewaySecret());
        headers.set("X-User-Id", subject);
        headers.set("X-User-Installation", user.assignedInstallationCode());
        return restTemplate.exchange(trimTrailingSlash(properties.getBaseUrl()) + path, HttpMethod.POST,
                new HttpEntity<>(payload, headers), JsonNode.class);
    }

    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Django readiness API integration is disabled.");
        }
        if (!StringUtils.hasText(properties.getGatewaySecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Django readiness API gateway credential is not configured.");
        }
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
