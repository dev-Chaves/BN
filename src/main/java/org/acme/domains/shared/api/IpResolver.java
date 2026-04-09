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

        String ip = request.getHeader("X-Forwarded-For");

        String host = request.getHeader("X-Real-IP");

        if(ip != null && !ip.isBlank()){
            return normalize(ip);
        }

        if(host != null && !host.isBlank()){
            return normalize(host);
        }

        return normalize(request.remoteAddress().host());

    }

    private String normalize(String ip){
        if(ip == null || ip.isBlank()){
            return null;
        }
        return ip.split(",")[0].trim();
    }

}
