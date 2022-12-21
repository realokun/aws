package com.aws.vokunev.prodcatalog.controller;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.aws.vokunev.prodcatalog.model.AppConfig;
import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;

/**
 * This controller handles the logout sequence for the Cognito User
 * Pool-authenticated user in accordance with:
 * https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-authenticate-users.html#authentication-logout
 */
@Controller
public class LogoutController {

    @Autowired
    private RequestScopeConfig requestScope;
    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(LogoutController.class);
    }

    @GetMapping("/logout")
    public void logout(HttpServletResponse response) throws ServletException, IOException {

        // retrieve current application configuration
        AppConfig appConfig = requestScope.getAppConfig();

        // expire the ALB cookies
        logger.info("Expiring the ALB cookies:");

        Cookie cookie1 = new Cookie("AWSELBAuthSessionCookie", "");
        cookie1.setMaxAge(-1);
        cookie1.setPath("/");
        response.addCookie(cookie1);
        logger.info(cookie1.getName());

        Cookie cookie2 = new Cookie("AWSELBAuthSessionCookie-0", "");
        cookie2.setMaxAge(-1);
        cookie2.setPath("/");
        response.addCookie(cookie2);
        logger.info(cookie2.getName());

        Cookie cookie3 = new Cookie("AWSELBAuthSessionCookie-1", "");
        cookie3.setMaxAge(-1);
        cookie3.setPath("/");
        response.addCookie(cookie3);
        logger.info(cookie3.getName());

        // redirect to the logout endpoint address
        response.sendRedirect(appConfig.getServiceEndpointLogout());
    }
}
