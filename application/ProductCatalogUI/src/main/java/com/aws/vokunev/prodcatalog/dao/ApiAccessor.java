package com.aws.vokunev.prodcatalog.dao;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;
import com.jayway.jsonpath.JsonPath;

/**
 * This is a base class for the data accessors retrieving the data from a web
 * API.
 */
@Component
@PropertySource("classpath:application.properties")
public abstract class ApiAccessor {

    @Autowired
    private RequestScopeConfig requestScope;
    @Value("${http.header.log.correlationid.egress}")
    private String correlationIdHeaderName;    
    @Value("${http.header.api.key}")
    private String apiKeyHeaderName;    
    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(ApiAccessor.class);
    }

    // HTTP methods supported by this accessor
    enum HttpMethod {
        GET, PUT;
    }

    /**
     * Sends a GET request to the provided URL.
     * 
     * @param url - the url of the API
     * @return the result of an API invocation or null if not available
     */
    protected String invokeGetAPIRequest(String url) throws IOException {
        return invokeGetAPIRequest(url, null);
    }

    /**
     * Sends a GET request to the provided URL with provided API key.
     * 
     * @param url    - the url of the API
     * @param apiKey - the value of the API key
     * @return the result of an API invocation or null if not available
     */
    protected String invokeGetAPIRequest(String url, String apiKey) throws IOException {
        return invokeAPIRequest(HttpMethod.GET, url, apiKey);
    }

    /**
     * Sends a PUT request to the provided URL with provided API key.
     * 
     * @param url    - the url of the API
     * @param apiKey - the value of the API key
     * @return the result of an API invocation or null if not available
     */
    protected String invokePutAPIRequest(String url, String apiKey) throws IOException {
        return invokeAPIRequest(HttpMethod.PUT, url, apiKey);
    }

    /**
     * Sends a specified HTTP request to the provided URL with provided API key.
     * 
     * @param url    - the url of the API
     * @param apiKey - the value of the API key
     * @return the result of an API invocation or null if not available
     */
    protected String invokeAPIRequest(HttpMethod method, String url, String apiKey) throws IOException {

        int timeoutSeconds = 5;

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpRequestBase request = null;
        switch (method) {
            case GET:
                request = new HttpGet(url);
                break;
            case PUT:
                request = new HttpPut(url);
                break;
        }

        // include log correlation id
        request.setHeader(correlationIdHeaderName, requestScope.getLogCorrelationId());  
        logger.info(String.format("Set egress request header %s=%s", correlationIdHeaderName, requestScope.getLogCorrelationId()));

        // include an API key if available
        if (apiKey != null) {
            request.setHeader(apiKeyHeaderName, apiKey);
        }

        int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000; // Timeout in millis.
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MS).setSocketTimeout(CONNECTION_TIMEOUT_MS).build();

        request.setConfig(requestConfig);

        // send the Get request
        logger.info(String.format("Sending HTTP GET request: %s", request));
        CloseableHttpResponse response = httpClient.execute(request);

        // log the response Status
        logger.info(String.format("HTTP response status: %s", response.getStatusLine().toString()));

        // process the response
        String result = EntityUtils.toString(response.getEntity());
        if (result == null) {
            throw new RuntimeException("Unexpected null value for API response entity.");
        }
        logger.info(String.format("HTTP response data: %s", result));

        // treat any response code other than 200 as an error
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException(response.getStatusLine().toString());
        }

        try {
            String error = JsonPath.read(result, "$.errorMessage");
            throw new RuntimeException(error);
        } catch (com.jayway.jsonpath.PathNotFoundException ex) {
            // no error was returned, which is good, we can continue
        }

        return result;
    }
}