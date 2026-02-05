package br.com.recargapay.wallet.infrastructure.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

  public UUID getAuthenticatedCustomerId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof Jwt jwt) {
      return UUID.fromString(jwt.getSubject());
    }
    throw new UserNotAuthenticatedException();
  }
}
