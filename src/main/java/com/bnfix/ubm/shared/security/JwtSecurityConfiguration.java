package com.bnfix.ubm.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class JwtSecurityConfiguration {
    @Bean
    BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    @Profile("!test")
    RSAKey jwtKey(@Value("${app.jwt.private-key}") String privatePath,
                  @Value("${app.jwt.public-key}") String publicPath) {
        try {
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(readPem(privatePath, "PRIVATE KEY")));
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(readPem(publicPath, "PUBLIC KEY")));
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("bn-api").build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load configured JWT RSA keys", exception);
        }
    }

    @Bean
    @Profile("test")
    RSAKey testJwtKey() {
        try {
            java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            java.security.KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID("test").build();
        } catch (Exception exception) { throw new IllegalStateException("Unable to create test JWT key", exception); }
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey key) {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(key));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey key, @Value("${app.jwt.issuer:bn-api}") String issuer) {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
            decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuer));
            return decoder;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create JWT decoder", exception);
        }
    }

    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(JwtDecoder decoder) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        JwtGrantedAuthoritiesConverter groups = new JwtGrantedAuthoritiesConverter();
        groups.setAuthoritiesClaimName("groups");
        groups.setAuthorityPrefix("ROLE_");
        provider.setJwtAuthenticationConverter(jwt -> {
            var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                    jwt, groups.convert(jwt), jwt.getSubject());
            return authentication;
        });
        return provider;
    }

    @Bean
    UserDetailsService swaggerUserDetailsService(
            @Value("${app.swagger.auth.username:}") String username,
            @Value("${app.swagger.auth.password:}") String password,
            BCryptPasswordEncoder passwordEncoder) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Swagger Basic Auth credentials must be configured");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("SWAGGER")
                .build());
    }

    @Bean
    @Order(1)
    SecurityFilterChain swaggerSecurityChain(
            HttpSecurity http,
            UserDetailsService swaggerUsers,
            BCryptPasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider swaggerProvider = new DaoAuthenticationProvider(swaggerUsers);
        swaggerProvider.setPasswordEncoder(passwordEncoder);
        http.securityMatcher("/q/openapi/**", "/q/swagger-ui/**", "/q/swagger-ui.html")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .authenticationProvider(swaggerProvider)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtCookieAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/login", "/auth/logout", "/onboarding", "/actuator/health", "/error").permitAll().anyRequest().authenticated())
                .addFilterBefore(jwtFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private static byte[] readPem(String file, String type) throws IOException {
        String pem = Files.readString(Path.of(file), StandardCharsets.US_ASCII)
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }
}
