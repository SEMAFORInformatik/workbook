/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.semafor.intens.ws.controller;

import ch.semafor.intens.ws.service.UserDetailsServiceImpl;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.service.UserService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A controller for the token resource.
 * from spring-guides spring-security-samples/servlet/spring-boot/java/jwt
 * @author Josh Cummings
 */
@Profile("auth-jwt")
@RestController
@Configuration
@RequestMapping("/")
public class TokenController {
	Logger logger = LoggerFactory.getLogger(TokenController.class);

	@Value("${app.tokenExpiry:#{36000L}}")
	private Long expiry;

	@Autowired
	JwtEncoder encoder;
	@Autowired
	UserDetailsServiceImpl userDetailsService;
	@Autowired
	PasswordEncoder passwordEncoder;
  @Autowired
  UserService userService;
  @Autowired
  Environment environment;
	@PostMapping(value = "/token",produces = MediaType.APPLICATION_JSON_VALUE)
	public String token(Authentication authentication) {
		String scope;
		String username;
		String groups;
		try {
			logger.debug("authentication ", authentication.getName());
			scope = authentication.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.joining(" "));
			username = authentication.getName();
		} catch (NullPointerException ex) {
			scope = "<null>";
			username = "<null>";
		}
		final String baseUrl = ServletUriComponentsBuilder
				.fromCurrentContextPath().build().toUriString();
		Instant now = Instant.now();
		// @formatter:off
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(baseUrl)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(expiry))
				.subject(username)
				.claim("scope", scope)
				.build();
		// @formatter:on
		return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

	@PostMapping(value = "/login")
			public String login(@RequestBody Map<String, String> loginRequest)
			throws AccessDeniedException {

		var bypass = Arrays.asList(environment.getActiveProfiles()).contains("auth-none");

		UserDetails userDetails = userDetailsService.loadUserByUsername(
				loginRequest.get("username"));
		if(!bypass && !passwordEncoder.matches((CharSequence)
						loginRequest.get("password"),
					userDetails.getPassword())){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Invalid credentials");
		}

    Owner owner = userService.findOwnerByUsername(loginRequest.get("username"));
    if (!bypass && owner.isTotpEnabled()) {
      TimeProvider timeProvider = new SystemTimeProvider();
      CodeGenerator codeGenerator = new DefaultCodeGenerator();
      CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
      String token = loginRequest.get("token");
      if (token == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "No Authenticator code");
      }

      if (!verifier.isValidCode(owner.getTotpSecret(), token)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "Invalid Authenticator code");
      }
    }
     

		Authentication auth = new Authentication() {
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return userDetails.getAuthorities();
			}

			@Override
			public Object getCredentials() {
				return null;
			}

			@Override
			public Object getDetails() {
				return userDetails;
			}

			@Override
			public Object getPrincipal() {
				return (Principal)userDetails;
			}

			@Override
			public boolean isAuthenticated() {
				return false;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
				// empty
			}

			@Override
			public String getName() {
				return userDetails.getUsername();
			}
		};
		return token(auth);
	}
	static class LoginRequest {
		public String username;
		public String password;
	}
}
