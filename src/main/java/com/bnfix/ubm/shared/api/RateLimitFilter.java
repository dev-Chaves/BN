package com.bnfix.ubm.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lightweight in-process limiter matching the existing endpoint groups. */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int authPerSecond;
    private final int authPerMinute;
    private final int onboardingPerSecond;
    private final int onboardingPerMinute;
    private final int redemptionPerSecond;
    private final int redemptionPerMinute;

    public RateLimitFilter(
            @Value("${app.rate-limit.auth.per-second:5}") int authPerSecond,
            @Value("${app.rate-limit.auth.per-minute:30}") int authPerMinute,
            @Value("${app.rate-limit.onboarding.per-second:2}") int onboardingPerSecond,
            @Value("${app.rate-limit.onboarding.per-minute:10}") int onboardingPerMinute,
            @Value("${app.rate-limit.redemption.per-second:10}") int redemptionPerSecond,
            @Value("${app.rate-limit.redemption.per-minute:120}") int redemptionPerMinute) {
        this.authPerSecond = authPerSecond;
        this.authPerMinute = authPerMinute;
        this.onboardingPerSecond = onboardingPerSecond;
        this.onboardingPerMinute = onboardingPerMinute;
        this.redemptionPerSecond = redemptionPerSecond;
        this.redemptionPerMinute = redemptionPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = ruleFor(request);
        if (rule == null || allowed(request.getRemoteAddr(), rule)) {
            chain.doFilter(request, response);
            return;
        }
        log.warn(
                "Rate limit exceeded for {} {} (ip={}, rule={})",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                rule.name());
        response.setStatus(429);
        response.setHeader("Retry-After", "1");
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Rate limit exceeded\",\"status\":429}");
    }

    private boolean allowed(String ip, Rule rule) {
        long now = System.nanoTime();
        return windows.computeIfAbsent(ip + ":" + rule.name(), ignored -> new Window())
                .accept(now, rule);
    }

    private Rule ruleFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(path))
            return new Rule("auth", authPerSecond, authPerMinute);
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/onboarding".equals(path))
            return new Rule("onboarding", onboardingPerSecond, onboardingPerMinute);
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.startsWith("/redemptions/"))
            return new Rule("redemption", redemptionPerSecond, redemptionPerMinute);
        return null;
    }

    private record Rule(String name, int perSecond, int perMinute) {}

    private static final class Window {
        private long secondStarted;
        private long minuteStarted;
        private int secondCount;
        private int minuteCount;

        synchronized boolean accept(long now, Rule rule) {
            long second = Duration.ofSeconds(1).toNanos();
            long minute = Duration.ofMinutes(1).toNanos();
            if (now - secondStarted >= second) {
                secondStarted = now;
                secondCount = 0;
            }
            if (now - minuteStarted >= minute) {
                minuteStarted = now;
                minuteCount = 0;
            }
            if (secondCount >= rule.perSecond() || minuteCount >= rule.perMinute()) return false;
            secondCount++;
            minuteCount++;
            return true;
        }
    }
}
