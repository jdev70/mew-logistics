package mil.dod.mew.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the server-to-server Django readiness API integration. */
@ConfigurationProperties(prefix = "mew.readiness-api")
public class ReadinessApiProperties {
    private boolean enabled;
    private String baseUrl = "http://127.0.0.1:8000";
    private String gatewaySecret = "";
    private String localSubjectOverride = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getGatewaySecret() { return gatewaySecret; }
    public void setGatewaySecret(String gatewaySecret) { this.gatewaySecret = gatewaySecret; }
    public String getLocalSubjectOverride() { return localSubjectOverride; }
    public void setLocalSubjectOverride(String localSubjectOverride) { this.localSubjectOverride = localSubjectOverride; }
}
