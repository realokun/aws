package com.aws.vokunev.prodcatalog.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.model.AppConfig;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationRequest;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;

/**
 * This accessor implements implements a DAO patetrn for accessing the
 * application configuration from AWS AppConfig service. It uses the most basic
 * data retrieval approach to keep this demo application simple. It requests the
 * application configuration directly from the AWS AppConfig service for every
 * application request.
 * A better approach would be to cache the configuration data
 * to reduce the amount of API calls to the AWS AppConfig service. An example of
 * a more advanced implementation can be found at:
 * https://github.com/aws-samples/aws-appconfig-java-sample
 */
@Component
@PropertySource("classpath:release.properties")
public class AppConfigAccessor {

    @Autowired
    private SecretsAccessor secretsAccessor;
    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(AppConfigAccessor.class);
    }

    /*
     * This is a unique, user-specified ID to identify the client for the AWS
     * AppConfig configuration. This ID enables AWS AppConfig to deploy the
     * configuration in intervals, as defined in the deployment strategy. See:
     * https://docs.aws.amazon.com/appconfig/latest/userguide/appconfig-retrieving-
     * the-configuration.html
     */
    private static final String clientId = UUID.randomUUID().toString();

    @Value("${appconfig.application}")
    String application;
    @Value("${appconfig.environment}")
    String environment;
    @Value("${appconfig.profile}")
    String profile;

    private AppConfigClient client = AppConfigClient.create();

    private GetConfigurationResponse getConfigurationFromAPI(String application, String environment, String profile) {

        GetConfigurationResponse result = client.getConfiguration(GetConfigurationRequest.builder()
                .application(application).environment(environment).configuration(profile).clientId(clientId).build());

        return result;
    }

    /**
     * Retrieves an application configuration from AWS AppConfig service based on
     * the values specified in release.properties file.
     * 
     * @return Instance of ApplicationConfiguration object
     */
    public AppConfig getConfiguration() {

        logger.info(String.format(
                "Requesting AppConfig configuration for application=%s, environment=%s, configuration profile=%s",
                application, environment, profile));

        GetConfigurationResponse result = getConfigurationFromAPI(application, environment, profile);

        if (result.content() == null) {
            logger.error("AppConfig returned empty response");
            throw new RuntimeException("The application configuration is not available.");
        }
        String appConfigResponse = result.content().asUtf8String();
        logger.info(String.format("AppConfig response: %s", appConfigResponse));

        AppConfig appConfig = getConfiguration(appConfigResponse);

        if (appConfig.getApiKeySecret() != null) {
            // the APi invocation requires an API key, which has to be retrieved first
            appConfig.setApiKey(secretsAccessor.getSecret(appConfig.getApiKeySecret(), "apikey"));
        }

        return appConfig;
    }

    /**
     * Parses JSON document representing application configuration and creates an
     * instance of ApplicationConfiguration object.
     * 
     * @param applicationConfigurationJson JSON document representing application
     *                                     configuration
     * @return Instance of ApplicationConfiguration object
     */
    public AppConfig getConfiguration(String applicationConfigurationJson) {

        DocumentContext context = JsonPath.parse(applicationConfigurationJson);
        AppConfig config = new AppConfig();

        int totalInstanceMetadataAccessRoles = context.read("$.InstanceMetadataAccessRoles.length()");
        List<String> instanceMetadataAccessRoles = new ArrayList<String>(totalInstanceMetadataAccessRoles);
        for (int i = 0; i < totalInstanceMetadataAccessRoles; i++) {
            instanceMetadataAccessRoles.add(context.read(String.format("$.InstanceMetadataAccessRoles[%s]", i)));
        }
        config.setInstanceMetadataAccessRoles(instanceMetadataAccessRoles);

        int totalPriceUpdateRoles = context.read("$.PriceUpdateRoles.length()");
        List<String> priceUpdateRoles = new ArrayList<String>(totalPriceUpdateRoles);
        for (int i = 0; i < totalPriceUpdateRoles; i++) {
            priceUpdateRoles.add(context.read(String.format("$.PriceUpdateRoles[%s]", i)));
        }
        config.setPriceUpdateRoles(priceUpdateRoles);

        config.setApiKeySecret(context.read("$.APIKeySecret"));
        config.setServiceEndpointProductList(context.read("$.ServiceEndpointProductList"));
        config.setServiceEndpointProductDetails(context.read("$.ServiceEndpointProductDetails"));
        config.setServiceEndpointProductPriceUpdate(context.read("$.ServiceEndpointProductPriceUpdate"));
        config.setServiceEndpointLogout(context.read("$.ServiceEndpointLogout"));
        config.setItemColor(context.read("$.ItemColor"));
        config.setFeatureFlagPriceUpdate(context.read("$.FeatureFlagPriceUpdate"));
        logger.info(String.format("AppConfig response parsed: %s", config));

        return config;
    }
}
