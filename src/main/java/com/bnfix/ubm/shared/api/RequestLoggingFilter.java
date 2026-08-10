package com.bnfix.ubm.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs every request with a level based on the outcome: debug for success, warn for 4xx, error for 5xx. */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } catch (Exception exception) {
            log.error(
                    "{} {} failed with unhandled exception after {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsedMillis(started),
                    exception);
            throw exception;
        }
        int status = response.getStatus();
        long elapsed = elapsedMillis(started);
        if (status >= 500) {
            log.error("{} {} -> {} in {}ms", request.getMethod(), request.getRequestURI(), status, elapsed);
        } else if (status >= 400) {
            log.warn("{} {} -> {} in {}ms", request.getMethod(), request.getRequestURI(), status, elapsed);
        } else {
            log.debug("{} {} -> {} in {}ms", request.getMethod(), request.getRequestURI(), status, elapsed);
        }
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
