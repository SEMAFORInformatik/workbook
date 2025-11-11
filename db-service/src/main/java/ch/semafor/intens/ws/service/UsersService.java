package ch.semafor.intens.ws.service;

import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.model.swagger.ExGroup;
import ch.semafor.intens.ws.model.swagger.ExUser;
import ch.semafor.intens.ws.utils.IntensWsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/services/rest")
@Transactional
public class UsersService extends BaseServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

  /**
   * Get one user by name
   * @param username username of the user to get
   * @return The user
   */
  @GetMapping(path = "/users/{username}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema =
          @Schema(implementation = ExUser.class))
      })
  })
  public Map<String, Object> findByUsername(@PathVariable("username") String username) {
    try {
      User details = new User(userService.findOwnerByUsername(username));
      if (details != null) {
        return details.toMap();
      }
      return new HashMap<String, Object>();
    } catch (UsernameNotFoundException e) {
      throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Get one or many users
   *
   * @param username Optional username to search for
   * @return List of matched users
   */
  @Secured("ROLE_ADMIN")
  @GetMapping(path = "/users")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExUser.class)))
      })
  })
  public List<Owner> find(
      @RequestParam(value = "username", required = false) final String username) {
    String search = "";
    if (username != null && !username.isEmpty()) {
      search = username;
    }

    List<Owner> l = userService.findOwners(search);
    logger.debug("found {} users", l.size());
    return l;
  }

  /**
   * Get a redacted list of users with password and roles/groups empty
   * @param username Optional name of single user to get
   * @return A list of matched users
   */
  @GetMapping(path = "/userslist")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExUser.class)))
      })
  })
  public List<Owner> findlist(
	  @RequestParam(value = "username", required = false) final String username) {
    String search = "";
    if (username != null && !username.isEmpty()) {
        search = username;
      }

    List<Owner> ol = userService.findOwners(search);
    List<Owner> ul = new ArrayList<Owner>();
    for(Owner o: ol) {
        if( o.isEnabled()) {
            Owner u = new Owner();
            u.setUsername(o.getUsername());
            u.setFirstName(o.getFirstName());
            u.setLastName(o.getLastName());
            u.setEnabled(true);
            ul.add(u);
        }
    }
    logger.debug("found {} users", ul.size());
    return ul;
  }

  /**
   * Get a list of all groups
   * @param onlyInUsers
   * @return A list of groups
   */
  @Secured("ROLE_ADMIN")
  @GetMapping(path = "/users/groups")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExGroup.class)))
      })
  })
  public List<Group> findAllGroups(
      @RequestParam(value = "onlyInUsers", defaultValue = "true") boolean onlyInUsers) {
    return userService.getGroups(onlyInUsers);
  }

  /**
   * Get info of the authorized user
   * @param authentication
   * @return The currently logged in user
   */
  @GetMapping("/user-info")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExUser.class))
      })
  })
  public Map<String, String> getUserInfo(Authentication authentication) {
    Map<String, String> map = new Hashtable<String, String>();
//          @AuthenticationPrincipal Jwt principal) {
    //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String currentPrincipalName = authentication.getName();
    try {
      Jwt principal = (Jwt) authentication.getPrincipal();
      map.put("user_name", principal.getClaimAsString("preferred_username"));
      map.put("given_name", principal.getClaimAsString("given_name"));
      map.put("family_name", principal.getClaimAsString("family_name"));
      map.put("resource_access", principal.getClaimAsString("resource_access"));
      map.put("realm_access", principal.getClaimAsString("realm_access"));
      // keycloak: client scope microprofile-jwt
      String groups = principal.getClaimAsString("groups");
      if( groups != null ) {
        map.put("groups", groups);
      }
      map.put("scope", principal.getClaimAsString("scope"));
      map.put("claims", principal.getClaims().keySet().toString());
    } catch(ClassCastException ex){
      User principal = (User) authentication.getPrincipal();
      map.put("user_name", principal.getUsername());
      map.put("given_name", principal.getFirstName());
      map.put("family_name", principal.getLastName());
      map.put("active_group", principal.getActiveGroup().getName());
    }
    return map;
  }

  public Owner saveOwner(Authentication authentication) {
//          @AuthenticationPrincipal Jwt principal) {
    //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt principal;
    try {
      principal = (Jwt) authentication.getPrincipal();
    } catch(ClassCastException ex){
      return null;
    }
    //logger.info(principal.getClass().toString());
    /*
    Map<String, String> map = new Hashtable<String, String>();
    map.put("user_name", principal.getClaimAsString("preferred_username"));
    map.put("given_name", principal.getClaimAsString("given_name"));
    map.put("family_name", principal.getClaimAsString("family_name"));
    map.put("resource_access", principal.getClaimAsString("resource_access"));
    map.put("realm_access", principal.getClaimAsString("realm_access"));
    map.put("groups", principal.getClaimAsString("groups"));
    map.put("scope", principal.getClaimAsString("scope"));
    map.put("claims", principal.getClaims().keySet().toString()); */

    Owner currentUser = new Owner(principal.getClaimAsString("preferred_username"));
    currentUser.setFirstName(principal.getClaimAsString("given_name"));
    currentUser.setLastName(principal.getClaimAsString("family_name"));
    for (String name : principal.getClaimAsStringList("groupMembership")) {
      currentUser.addGroup(new Group(name));
    }
    String activeGroup = principal.getClaimAsString("activeGroup");
    if (activeGroup != null) {
      currentUser.setActiveGroup(new Group(activeGroup));
    }
    return userService.saveOwner(currentUser);
  }
}
