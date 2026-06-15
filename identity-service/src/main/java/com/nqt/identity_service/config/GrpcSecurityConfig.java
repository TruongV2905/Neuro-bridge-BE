package com.nqt.identity_service.config;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * gRPC Security Configuration
 * Provides a GrpcAuthenticationReader that allows all requests with ANONYMOUS authentication
 * This is suitable for internal gRPC services that rely on HTTP security layer for authentication
 */
@Configuration
public class GrpcSecurityConfig {

  /**
   * Provides a GrpcAuthenticationReader that creates anonymous authentication for all gRPC
   * requests. This allows the gRPC service to function without requiring a specific security
   * interceptor while still maintaining Spring Security context.
   */
  @Bean
  public GrpcAuthenticationReader grpcAuthenticationReader() {
    return new GrpcAuthenticationReader() {
      @Override
      public Authentication readAuthentication(ServerCall<?, ?> call, Metadata headers) {
        // Allow all gRPC requests with ANONYMOUS authentication
        // Actual authentication/authorization can be enforced at the service method level if needed
        return new AnonymousAuthenticationToken(
            "grpc-anonymous-key",
            "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
      }
    };
  }
}

