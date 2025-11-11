package ch.semafor.intens.ws.service;

import ch.semafor.gendas.exceptions.GroupExistsException;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Role;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.model.swagger.ExChanges;
import ch.semafor.intens.ws.model.swagger.ExUser;
import ch.semafor.intens.ws.utils.IntensWsException;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;

@RestController
@RequestMapping("/services/rest")
@Transactional
public class UserManagementService extends BaseServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);

  @Value("${app.totpName:Semafor DB-Service Dashboard}")
  private String totpName;
  @Autowired
  PasswordEncoder encoder;

  /**
   * Save a user
   *
   * @param data The data of the user to save
   * @return The saved user
   */
  @PreAuthorize("#data['username'] == authentication.name or hasRole ( 'ROLE_ADMIN')")
  @PutMapping(path = "/users")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExUser.class))
      })
  })
  public Map<String, Object> save(@Parameter(schema = @Schema(implementation = ExUser.class)) @RequestBody Map<String, Object> data) {
    Owner owner;
    if (data.get("username") == null) {
      String message = "Username missing, cannot save user";
      logger.warn(message);
      throw new IntensWsException(message, HttpStatus.BAD_REQUEST);
    }
    String username = (String) data.get("username");
    logger.info("Update user {}  by {}", username,
            SecurityContextHolder.getContext().getAuthentication().getName());
    try {
      owner = userService.findOwnerByUsername(username);
      logger.debug("existing user {}", username);
    } catch (ch.semafor.gendas.exceptions.UsernameNotFoundException ex) {
      owner = new Owner();
      owner.setUsername(username);
      logger.debug("new user {}", username);
    }

    owner.setGroups(new HashSet<Group>());
    owner.setRoles(new HashSet<Role>());

    List<String> roles = null;
    if (data.get("authorities") != null) {
      roles = (List<String>) data.get("authorities");
      for (String rolename : roles) {
        owner.addRole(new Role(rolename));
      }
    } else if (data.get("roles") != null) {
      List<Map<String, String>> rolelist =
              (List<Map<String, String>>) data.get("roles");
      for (Map<String, String> r : rolelist) {
        if (!(r.get("name") == null || r.get("name").isEmpty())) {
          owner.addRole(new Role(r.get("name")));
        }
      }
    }

    List<String> groups = null;
    if (data.get("groups") != null) {
      List<Map<String, String>> grouplist =
              (List<Map<String, String>>) data.get("groups");
      for (Map<String, String> g : grouplist) {
        if (!(g.get("name") == null || g.get("name").isEmpty())) {
          owner.addGroup(new Group(g.get("name")));
        }
      }
    }

    // Check if we need update the password!
    if (data.get("password") == null) {
      logger.info("Password missing, cannot save user {}", username);
      throw new IntensWsException("Password missing, cannot save user " + username,
              HttpStatus.BAD_REQUEST);
    }
    if (!data.get("password").toString().equals(owner.getPassword())) {
      owner.setPassword(this.encryptPassword((String) data.get("password")));
    }

    owner.setUsername((String) data.get("username"));
    owner.setFirstName((String) data.get("firstName"));
    owner.setLastName((String) data.get("lastName"));
    if (data.get("enabled") instanceof Boolean) {
      owner.setEnabled((Boolean) data.get("enabled"));
    } else {
      Integer enabled = 0;
      if (data.get("enabled") != null) {
        enabled = (Integer) data.get("enabled");
      }
      owner.setEnabled(enabled != 0);
    }
    if (data.containsKey("activeGroup") && data.get("activeGroup") != null) {
      Map<String, String> ag = (Map<String, String>) data.get("activeGroup");
      owner.setActiveGroup(new Group(ag.get("name")));
    } else {
      owner.setActiveGroup(null);
    }
    logger.debug("save owner {}", owner);
    User user = new User(userService.saveOwner(owner));
    return user.toMap();
  }


  @Secured("ROLE_ADMIN")
  @PutMapping(path = "/users/{username}/activateTOTP")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public String activateTOTP(@PathVariable("username") String username, @RequestBody Map<String, Object> data) {
    String code = (String) data.get("code");
    TimeProvider timeProvider = new SystemTimeProvider();
    CodeGenerator codeGenerator = new DefaultCodeGenerator();
    CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    Owner owner = userService.findOwnerByUsername(username);
    String secret = owner.getTotpSecret();

    if (verifier.isValidCode(secret, code)) {
      owner.setTotpEnabled(true);
      userService.saveOwner(owner);
    } else {
      throw new IntensWsException("Invalid 2FA Code", HttpStatus.UNAUTHORIZED);
    }
    return "";
  }

  @Secured("ROLE_ADMIN")
  @PutMapping(path = "/users/{username}/deactivateTOTP")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public String deactivateTOTP(@PathVariable("username") String username) {
    Owner owner = userService.findOwnerByUsername(username);
    owner.setTotpEnabled(false);
    userService.saveOwner(owner);
    return "";
  }

  @Secured("ROLE_ADMIN")
  @PutMapping(path = "/users/{username}/initializeTOTP")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public String initializeTOTP(@PathVariable("username") String username) throws QrGenerationException {
    logger.info("activating totp for user: ", username);
    SecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    String secret = secretGenerator.generate();

    Owner user = userService.findOwnerByUsername(username);
    if (user.isTotpEnabled()) {
      throw new IntensWsException("User already has 2FA Enabled", HttpStatus.CONFLICT);
    }
    user.setTotpSecret(secret);
    userService.saveOwner(user);
    QrData data = new QrData.Builder()
      .label(username)
      .secret(secret)
      .issuer(this.totpName)
      .algorithm(HashingAlgorithm.SHA1)
      .digits(6)
      .period(30)
      .build();

    QrGenerator generator = new ZxingPngQrGenerator();
    byte[] imageData = generator.generate(data);
    String mimeType = generator.getImageMimeType();

    String dataUri = dev.samstevens.totp.util.Utils.getDataUriForImage(imageData, mimeType);

    return secret + ";" + dataUri;
  }

  /**
   * Rename a group
   *
   * @param actual Current name of the group
   * @param newName New name of the group
   * @return The amount of users who had their groups changed
   */
  @Secured("ROLE_ADMIN")
  @PutMapping(path = "/users/groups/rename/{actual}/{newName}")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExChanges.class))
      })
  })
  public Map<String, Object> renameGroup(@PathVariable("actual") String actual,
                                         @PathVariable("newName") String newName) {
    Map<String, Object> ret = new HashMap<String, Object>();

    if (actual.equals(newName)) {
      return ret;
    }
    logger.info("Rename group from {} to {}", actual, newName);
    try {
      ret.put("changes", userService.renameGroup(actual, newName));
    } catch (GroupExistsException ex) {
      throw new IntensWsException(ex, HttpStatus.BAD_REQUEST);
    }
    logger.info(ret.toString());
    return ret;
  }

  /**
   * Change the authorized user's password
   * @param password the new password
   * @return User object with the new password's hash
   */
  @PutMapping(path = "/users/password")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExUser.class))
      })
  })
  public Map<String, Object> update_password(@RequestBody String password) {
    Owner owner = getOwner();
    // Use bcrypt strength 11
    owner.setPassword(encryptPassword(stripQuotationChars(password)));
    User user = new User(userService.saveOwner(owner));
    return user.toMap();
  }

  private String encryptPassword(String password) {
    return this.encoder.encode(password);
  }
}
