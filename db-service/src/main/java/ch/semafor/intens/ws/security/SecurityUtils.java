package ch.semafor.intens.ws.security;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.semafor.gendas.model.Owner;
import ch.semafor.intens.ws.config.AuthoritiesConstants;
import ch.semafor.intens.ws.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Utility class for Spring Security. (copied from jhipster.tech)
 */
public final class SecurityUtils {

    private static Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    //public static final String CLAIMS_NAMESPACE = "https://www.jhipster.tech/";

    private SecurityUtils() {}

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static String getCurrentUserLogin() {
        String principal = extractPrincipal();
        if (principal == null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return principal;
    }
    public static Map<String, Object> getPrincipalAttributes() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        Map<String, Object> attrs = new HashMap<>();
        if (authentication == null) {
            attrs.put("preferred_username", null);
        }
        else if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
            attrs.put("preferred_username", springSecurityUser.getUsername());
        } else if (authentication instanceof JwtAuthenticationToken) {
            attrs= ((JwtAuthenticationToken) authentication)
                    .getToken().getClaims();
        } else if (authentication.getPrincipal() instanceof DefaultOidcUser) {
            attrs = ((DefaultOidcUser) authentication.getPrincipal()).getAttributes();
        } else if (authentication.getPrincipal() instanceof String) {
            attrs.put("preferred_username", (String) authentication.getPrincipal());
        }
        return attrs;
    }

    private static String extractPrincipal() {
        return (String) getPrincipalAttributes().get("preferred_username");
    }

    public static void setPrincipal(User user){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(
                new Authentication(){

            @Override
            public String getName() {
                return auth.getName();
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return auth.getAuthorities();
            }

            @Override
            public Object getCredentials() {
                return auth.getCredentials();
            }

            @Override
            public Object getDetails() {
                return auth.getDetails();
            }

            @Override
            public Object getPrincipal() {
                return user;
            }

            @Override
            public boolean isAuthenticated() {
                return auth.isAuthenticated();
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
                auth.setAuthenticated(isAuthenticated);
            }
        });
    }
    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
    }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (
            authentication != null && getAuthorities(authentication).anyMatch(authority -> Arrays.asList(authorities).contains(authority))
        );
    }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
        return !hasCurrentUserAnyOfAuthorities(authorities);
    }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    public static boolean hasCurrentUserThisAuthority(String authority) {
        return hasCurrentUserAnyOfAuthorities(authority);
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication instanceof JwtAuthenticationToken
            ? extractAuthorityFromClaims(((JwtAuthenticationToken) authentication).getToken().getClaims())
            : authentication.getAuthorities();
        return authorities.stream().map(GrantedAuthority::getAuthority);
    }

    public static List<GrantedAuthority> extractAuthorityFromClaims(Map<String, Object> claims) {
        return mapRolesToGrantedAuthorities(getRolesFromClaims(claims));
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> getRolesFromClaims(Map<String, Object> claims) {
        return (Collection<String>)
                /* claims.getOrDefault("groups", */
            claims.getOrDefault("roles", new ArrayList<>()
//                    claims.getOrDefault(CLAIMS_NAMESPACE + "roles", new ArrayList<>()))
        );
    }

    private static List<GrantedAuthority> mapRolesToGrantedAuthorities(Collection<String> roles) {
        return roles.stream().filter(role -> role.startsWith("ROLE_")).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
