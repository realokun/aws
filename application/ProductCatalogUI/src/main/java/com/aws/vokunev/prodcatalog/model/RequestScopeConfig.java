package com.aws.vokunev.prodcatalog.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * An object of this type represents a request-scoped configuration.
 */
@Component
@RequestScope
public class RequestScopeConfig {

    private String logCorrelationId;
    private InstanceMetadata metadata;
    private AccessToken accessToken;
    private AppConfig appConfig;

    public String getLogCorrelationId() {
        return logCorrelationId;
    }

    public void setLogCorrelationId(String logCorrelationId) {
        this.logCorrelationId = logCorrelationId;
    }

    public InstanceMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(InstanceMetadata metadata) {
        this.metadata = metadata;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public String formatLogMsg(String message) {
        return String.format("(%s) %s", logCorrelationId, message);
    }
}
