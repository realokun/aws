package com.aws.vokunev.prodcatalog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;

/**
 * This is a simple wrapper around the org.slf4j.Logger. It injects a log
 * correlation id in every log message. Each message starts with the "\n"
 * character to staisfy the "multi_line_start_pattern" of the CloudWatch agent.
 * This way, each log mesaage will be logged separately, see:
 * https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AgentReference.html
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
        LOGGER.info("\n" + requestScope.formatLogMsg(message));
    }

    public void error(String message) {
        LOGGER.error("\n" + requestScope.formatLogMsg(message));
    }

    public void error(String message, Exception ex) {
        LOGGER.error("\n" + requestScope.formatLogMsg(message), ex);
    }
}
