package ch.semafor.intens.ws.controller;

import ch.semafor.intens.ws.service.LiquibaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/services/liquibase")
@Transactional(readOnly = true)
public class LiquibaseController{

  private static final Logger logger = LoggerFactory.getLogger(LiquibaseController.class);

  @Autowired
  private Environment environment;

  @Autowired
  private LiquibaseService migrationService;

  @Secured("ROLE_ADMIN") // or @PreAuthorize(hasAuthority("SCOPE_admin")
  @PostMapping(path = "/migration")
  @Transactional
  public ResponseEntity updateDatabase() {
    logger.debug("Update database");
    var allowed = false;
    for (String profile : environment.getActiveProfiles()) {
      if (profile.equals("dev")) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("This feature can only be used in production mode");
      }
      if (profile.equals("liquibase")) {
        allowed = true;
      }
    }
    if (!allowed) {
      return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("To enable this feature, please turn on the liquibase profile");
    }

    boolean executed = migrationService.executeMigration();
    logger.info("Database migration executed: {}", executed);

    if(executed) {
      return ResponseEntity.status(HttpStatus.OK)
          .body("The database update was executed successfully");
    } else  {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("There was an error during the database update");
    }
  }

  @Secured({"ROLE_USER", "ROLE_ADMIN" })
  @GetMapping(path = "/types")
  public Map<String, List<Map<String, Object>>> getTypesFlat() {
    return migrationService.createTypeMap();
  }
}
