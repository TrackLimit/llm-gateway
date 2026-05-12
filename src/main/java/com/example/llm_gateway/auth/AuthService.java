package com.example.llm_gateway.auth;

import com.example.llm_gateway.auth.AuthDtos.LoginRequest;
import com.example.llm_gateway.auth.AuthDtos.RefreshRequest;
import com.example.llm_gateway.auth.AuthDtos.SignupRequest;
import com.example.llm_gateway.auth.AuthDtos.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final OrganizationRepository orgs;
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwt;
  private final RefreshTokenStore refreshStore;

  @Transactional
  public TokenResponse signup(SignupRequest req) {
    if (users.existsByEmail(req.email())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
    }
    var org = orgs.save(new Organization(req.orgName()));
    var user =
        users.save(new User(org.getId(), req.email(), passwordEncoder.encode(req.password())));
    return issuePair(user);
  }

  public TokenResponse login(LoginRequest req) {
    var user =
        users
            .findByEmail(req.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return issuePair(user);
  }

  public TokenResponse refresh(RefreshRequest req) {
    var userId =
        refreshStore
            .consume(req.refreshToken())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    var user =
        users
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    return issuePair(user);
  }

  private TokenResponse issuePair(User user) {
    var access = jwt.issueAccessToken(user.getId(), user.getOrgId());
    var refresh = refreshStore.issue(user.getId());
    return new TokenResponse(access, refresh);
  }
}
