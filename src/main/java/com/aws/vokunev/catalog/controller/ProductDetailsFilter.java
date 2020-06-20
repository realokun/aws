package com.aws.vokunev.catalog.controller;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

/*
 * Validates the input parameter for the servlet
 * Note: The annotation @WebFilter(urlPatterns = {"/product"}) works on Tomcat 9.x,
 * but fails on Tomcat 8.x, which is currently supported major version by
 * AWS Elastic Beanstalk service. Therefore we'll declare our filter in web.xml file.
 */
public class ProductDetailsFilter implements Filter {
   
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException { 
	  
    // Check if the parameter is present
    String param = request.getParameter("id");
    if (param == null ) {
        throw new RuntimeException("Error: missing request parameter \"id\"");
    }

    // Parse the input parameter as int
    try {
        Integer.parseInt(param);
    } catch (java.lang.NumberFormatException ex) {
        throw new RuntimeException("Error: unable to parse value " + param + " as integer.");
    }
         
      // Pass request back down the filter chain 
      chain.doFilter(request,response); 
   }
}