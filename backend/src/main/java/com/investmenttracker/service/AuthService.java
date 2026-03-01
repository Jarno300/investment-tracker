package com.investmenttracker.service;

import com.investmenttracker.config.JwtProperties;
import com.investmenttracker.dto.AuthRequest;
import com.investmenttracker.dto.AuthResponse;
import com.investmenttracker.dto.RefreshRequest;
import com.investmenttracker.dto.RegisterRequest;
import com.investmenttracker.dto.UserResponse;
import com.investmenttracker.model.Role;
import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final HoldingRepository holdingRepository;
  private final StockSearchService stockSearchService;
  private final StockQuoteCacheService stockQuoteCacheService;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtProperties jwtProperties;

  public AuthService(UserRepository userRepository,
      HoldingRepository holdingRepository,
      StockSearchService stockSearchService,
      StockQuoteCacheService stockQuoteCacheService,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder,
      JwtDecoder jwtDecoder,
      JwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.holdingRepository = holdingRepository;
    this.stockSearchService = stockSearchService;
    this.stockQuoteCacheService = stockQuoteCacheService;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.jwtProperties = jwtProperties;
  }

  public AuthResponse register(RegisterRequest request) {
    if (request == null || isBlank(request.email()) || isBlank(request.password())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
    }
    if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
    UserAccount user = new UserAccount();
    user.setEmail(request.email().trim().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRoles(Set.of(Role.USER));
    UserAccount saved = userRepository.save(user);
    return buildAuthResponse(saved);
  }

  public AuthResponse login(AuthRequest request) {
    if (request == null || isBlank(request.email()) || isBlank(request.password())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
    }
    UserAccount user = userRepository.findByEmailIgnoreCase(request.email().trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    refreshOwnedStockQuotesForToday(user);
    return buildAuthResponse(user);
  }

  public AuthResponse refresh(RefreshRequest request) {
    if (request == null || isBlank(request.refreshToken())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
    }
    Jwt decoded;
    try {
      decoded = jwtDecoder.decode(request.refreshToken());
    } catch (JwtException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }
    String tokenType = decoded.getClaimAsString("typ");
    if (!"refresh".equals(tokenType)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }
    String email = decoded.getSubject();
    if (isBlank(email)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }
    UserAccount user = userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
    return buildAuthResponse(user);
  }

  private AuthResponse buildAuthResponse(UserAccount user) {
    String accessToken = generateToken(user, "access", jwtProperties.getAccessTokenSeconds());
    String refreshToken = generateToken(user, "refresh", jwtProperties.getRefreshTokenSeconds());
    return new AuthResponse(
        accessToken,
        refreshToken,
        "Bearer",
        jwtProperties.getAccessTokenSeconds(),
        toUserResponse(user));
  }

  private String generateToken(UserAccount user, String type, long ttlSeconds) {
    Instant now = Instant.now();
    JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("investment-tracker")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(ttlSeconds))
        .subject(user.getEmail())
        .claim("uid", user.getId())
        .claim("typ", type)
        .claim("roles", user.getRoles().stream().map(Role::name).toList())
        .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
  }

  private UserResponse toUserResponse(UserAccount user) {
    Set<String> roles = user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
    return new UserResponse(user.getId(), user.getEmail(), roles);
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private void refreshOwnedStockQuotesForToday(UserAccount user) {
    List<String> symbols = holdingRepository.findDistinctOwnedStockSymbolsByUserId(user.getId());
    for (String symbol : symbols) {
      if (stockQuoteCacheService.wasRefreshedToday(symbol)) {
        continue;
      }
      try {
        stockSearchService.refreshQuote(symbol);
      } catch (Exception ex) {
        // Login should not fail because quote refresh failed.
        log.warn("Failed to refresh quote cache for [{}] on login for user [{}]", symbol, user.getId(), ex);
      }
    }
  }
}
