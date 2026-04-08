package org.acme.domains.shared.api;

import io.quarkiverse.bucket4j.runtime.resolver.IdentityResolver;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IpResolver implements IdentityResolver {

    private final HttpServerRequest request;

    public IpResolver(HttpServerRequest request) {
        this.request = request;
    }

    @Override
    public String getIdentityKey() {

        String ip = extractIp(request.getHeader("X-Forwarded-For"));

        if(ip != null) {
            return ip;
        }

        ip = extractIp(request.getHeader("Proxy-Client-IP"));

        if(!ip.isBlank()) {
            return ip;
        }

        return extractIp(request.remoteAddress().host());

    }

    private String extractIp(String ip){
        if(ip == null || ip.isBlank()){
            return null;
        }
        return ip.split(",")[0].trim();
    }

}
