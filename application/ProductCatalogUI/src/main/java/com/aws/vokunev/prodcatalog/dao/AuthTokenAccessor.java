package com.aws.vokunev.prodcatalog.dao;

import java.util.ArrayList;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.model.AccessToken;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

/**
 * This class implements a DAO patern for retrieving an OIDC access token from
 * the request.
 */
@Component
@PropertySource("classpath:application.properties")
public class AuthTokenAccessor {

    @Value("${http.header.oidc.access.token}")
    private String oidcAccessTokenHeaderName;
    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(AuthTokenAccessor.class);
    }

    /**
     * This method attempts to locate and fetch the user data from an OIDC access
     * token forwarded by an Application Load Balancer as per
     * https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html
     * 
     * @return an instance of a {@link AccessToken} or null if not available.
     */
    public AccessToken getToken(HttpServletRequest request) {
        return getToken(request.getHeader(oidcAccessTokenHeaderName));
    }

    /**
     * This method parses provided Base64 encoded OIDC access token. Please note
     * that this is a simplified token consumption algorithm. For the proper token
     * treatment please refer to:
     * https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html
     * 
     * @return an instance of a {@link AccessToken} or null if not available.
     */
    public AccessToken getToken(String access_token_encoded) {

        logger.info(String.format("Encoded access token received: %s", access_token_encoded));

        if (access_token_encoded == null) {
            return null;
        }

        AccessToken user = new AccessToken();

        // decode the token
        String[] parts = access_token_encoded.split("\\.");
        String b64payload = parts[1];
        byte[] decodedBytes = Base64.getDecoder().decode(b64payload);
        String access_token_decoded = new String(decodedBytes);
        logger.info(String.format("Access token decoded: %s", access_token_decoded));

        // process the token
        DocumentContext context = JsonPath.parse(access_token_decoded);

        try {
            // parse user name
            user.setUsername(context.read("$.username"));
        } catch (com.jayway.jsonpath.PathNotFoundException ex) {
            // if the element is not found, just log the exception and carry on
            logger.error("No username was found in the access token", ex);
        }

        try {
            // parse user groups
            JSONArray groups_array = (JSONArray) context.read("$.cognito:groups");
            if (groups_array != null) {
                ArrayList<String> groups = new ArrayList<String>(groups_array.size());
                for (Object group : groups_array) {
                    groups.add(String.valueOf(group));
                }
                user.setGroups(groups);
            }
        } catch (com.jayway.jsonpath.PathNotFoundException ex) {
            // if the element is not found, just log the exception and carry on
            logger.error("No user groups were found in the access token", ex);
        }

        return user;
    }
}
