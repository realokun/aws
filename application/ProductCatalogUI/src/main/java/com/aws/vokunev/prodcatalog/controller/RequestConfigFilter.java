package com.aws.vokunev.prodcatalog.controller;

import java.io.IOException;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.dao.AppConfigAccessor;
import com.aws.vokunev.prodcatalog.dao.AuthTokenAccessor;
import com.aws.vokunev.prodcatalog.dao.InstanceMetadataAccessor;
import com.aws.vokunev.prodcatalog.model.AccessToken;
import com.aws.vokunev.prodcatalog.model.AppConfig;
import com.aws.vokunev.prodcatalog.model.InstanceMetadata;
import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;

/**
 * This web filter creates a request-scoped configuration for the request.
 * 
 * How to Define a Spring Boot Filter:
 * https://www.baeldung.com/spring-boot-add-filter
 */
@Component
@PropertySource("classpath:application.properties")
@Order(1)
public class RequestConfigFilter implements Filter {

    @Autowired
    private RequestScopeConfig requestScope;
    @Autowired
    private AuthTokenAccessor accessTokenDataAccessor;
    @Autowired
    private InstanceMetadataAccessor instanceMetadataAccessor;
    @Autowired
    private AppConfigAccessor appConfigAccessor;
    @Autowired
    private CorrelatingLogger logger;
    @Value("${http.header.log.correlationid.ingress}")
    private String correlationIdHeaderName;

    @PostConstruct
    private void init() {
        logger.init(RequestConfigFilter.class);
    }

    /**
     * Initialize the request scope attributes.
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String urlString = req.getRequestURL().toString();
        if (urlString.contains("/images/") || urlString.contains("/styles/")) {
            // ignore requests to the static resources
            chain.doFilter(request, response);
        } else {
            // extract a log correlation id from the request
            String logCorrelationId = req.getHeader(correlationIdHeaderName);
            boolean isProvided = true;
            if (logCorrelationId == null) {
                // generate new if not provided
                logCorrelationId = UUID.randomUUID().toString();
                isProvided = false;
            }
            requestScope.setLogCorrelationId(logCorrelationId);
            logger.info(String.format("Begin processing request %s", urlString));
            if (isProvided) {
                logger.info(String.format("The log correlation id %s was provided with the request header %s",
                        logCorrelationId, correlationIdHeaderName));
            } else {
                logger.info(String.format(
                        "The log correlation id was not provided with the request header %s. Had to generate a new value of %s",
                        correlationIdHeaderName, logCorrelationId));
            }

            // extract an ALB access token from the request
            AccessToken access_token = accessTokenDataAccessor.getToken((HttpServletRequest) request);
            requestScope.setAccessToken(access_token);

            // load the EC2 instance metadata for the authenticated requests only
            if (access_token != null) {
                InstanceMetadata metadata = instanceMetadataAccessor.getInstanceMetadata();
                requestScope.setMetadata(metadata);
            }

            // load the application configuration
            AppConfig appConfig = appConfigAccessor.getConfiguration();
            requestScope.setAppConfig(appConfig);

            // pass the request along the filter chain
            chain.doFilter(request, response);
        }
    }
}
