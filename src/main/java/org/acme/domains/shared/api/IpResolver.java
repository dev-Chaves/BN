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

        return normalize(request.remoteAddress().host());

    }

    private String normalize(String ip){
        if(ip == null || ip.isBlank()){
            return null;
        }
        return ip.trim();
    }

}
