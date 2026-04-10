package com.bet99.bugtracker.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Attaches a short unique request ID to every log line via MDC.
 * The ID is also returned in the X-Request-Id response header so
 * client-side errors can be correlated with server logs.
 */
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String MDC_KEY = "reqId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq = (HttpServletRequest)  req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        String reqId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, reqId);
        httpRes.setHeader("X-Request-Id", reqId);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = System.currentTimeMillis() - start;
            log.debug("{} {} -> {} ({}ms)",
                    httpReq.getMethod(), httpReq.getRequestURI(),
                    httpRes.getStatus(), ms);
            MDC.remove(MDC_KEY);
        }
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
