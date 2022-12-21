package com.aws.vokunev.prodcatalog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;

/**
 * This is a simple wrapper around the org.slf4j.Logger. It injects a log
 * correlation id in every log message.S
 */
@Component
public class CorrelatingLogger {

    @Autowired
    private RequestScopeConfig requestScope;

    private Logger LOGGER;

    public void init(Class<?> clazz) {
        LOGGER = LoggerFactory.getLogger(clazz);
    }

    public void info(String message) {
        LOGGER.info(requestScope.formatLogMsg(message));
    }

    public void error(String message) {
        LOGGER.error(requestScope.formatLogMsg(message));
    }

    public void error(String message, Exception ex) {
        LOGGER.error(requestScope.formatLogMsg(message), ex);
    }
}
