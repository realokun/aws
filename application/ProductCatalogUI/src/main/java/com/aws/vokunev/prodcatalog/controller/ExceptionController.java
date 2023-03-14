package com.aws.vokunev.prodcatalog.controller;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;

/**
 * This exception controller replaces the default exception controller. It
 * provides a log correlation Id that can be used to correlate the logs in
 * the distributed system.
 */
@ControllerAdvice
public class ExceptionController {

    @Autowired
    private RequestScopeConfig requestScope;
    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(ExceptionController.class);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception exception) {
        logger.error(ExceptionUtils.getStackTrace(exception));
        return getModelAndView(exception, "error");
    }

    private ModelAndView getModelAndView(Exception ex, String view) {
        ModelAndView mav = new ModelAndView();
        mav.getModel().put("message", ex.getMessage());
        mav.getModel().put("correlationId", requestScope.getLogCorrelationId());
        mav.setViewName(view);
        return mav;
    }
}
