package org.acme.domains.shared.api;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class IpResolverTest {

    @Test
    void shouldIgnoreUntrustedForwardedIp() {
        HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" 10.0.0.10, 10.0.0.11 ");
        SocketAddress remoteAddress = Mockito.mock(SocketAddress.class);
        when(request.remoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.host()).thenReturn("127.0.0.1");

        IpResolver resolver = new IpResolver(request);

        assertEquals("127.0.0.1", resolver.getIdentityKey());
    }

    @Test
    void shouldIgnoreUntrustedRealIp() {
        HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getHeader("X-Real-IP")).thenReturn(" 172.16.0.1 ");
        SocketAddress remoteAddress = Mockito.mock(SocketAddress.class);
        when(request.remoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.host()).thenReturn("127.0.0.1");

        IpResolver resolver = new IpResolver(request);

        assertEquals("127.0.0.1", resolver.getIdentityKey());
    }

    @Test
    void shouldFallbackToRemoteHostWhenHeadersAreMissing() {
        HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
        SocketAddress remoteAddress = Mockito.mock(SocketAddress.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.remoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.host()).thenReturn("127.0.0.1");

        IpResolver resolver = new IpResolver(request);

        assertEquals("127.0.0.1", resolver.getIdentityKey());
    }

    @Test
    void shouldAlwaysUseResolvedRemoteAddress() {
        HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");
        when(request.getHeader("X-Real-IP")).thenReturn("172.16.0.1");
        SocketAddress remoteAddress = Mockito.mock(SocketAddress.class);
        when(request.remoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.host()).thenReturn("127.0.0.1");

        IpResolver resolver = new IpResolver(request);

        assertEquals("127.0.0.1", resolver.getIdentityKey());
    }
}
