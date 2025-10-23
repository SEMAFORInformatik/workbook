package ch.semafor.intens.ws.service;

import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.intens.ws.config.AppProperties;
import jakarta.activation.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.DirectoryResourceAccessor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LiquibaseService {

  private static final Logger logger = LoggerFactory.getLogger(LiquibaseService.class);

  @Autowired
  private ElementService elementService;

  @Autowired
  private AppProperties properties;

  @Autowired
  JdbcTemplate jdbcTemplate;

  public boolean executeMigration() {
    Liquibase liquibase = null;
    boolean status = false;
    try {
      // Caution: directory separator!
      Path migrationDir = Paths.get(System.getProperty("java.io.tmpdir"),
          properties.getMigration().getDirectory());

      CredentialsProvider cred = new UsernamePasswordCredentialsProvider(
          properties.getMigration().getUsername(),
          properties.getMigration().getToken());
      if(this.pullChangelogRepo(cred, migrationDir)) {

        liquibase = new Liquibase(
            properties.getMigration().getMasterFile(),
            new DirectoryResourceAccessor(migrationDir.toFile()),
            this.initDatabaseConnection());
        liquibase.update(new Contexts());
        status = true;
      }
    } catch(Exception e){
      logger.error(e.getMessage(), e);
    }
    finally {
      try {
        if (liquibase != null)
          liquibase.close();
      }catch(Exception e){}
    }
    return status;
  }

  /**
   * Creates Map of ElementTypes, PropertyTypes, their connections and references
   * @return created elementype
   */
  public Map<String, List<Map<String, Object>>> createTypeMap() {
    var elementTypeList = new ArrayList<Map<String, Object>>();
    var propertyTypeList = new ArrayList<Map<String, Object>>();
    var connectionList = new ArrayList<Map<String, Object>>();
    var referencesList = new ArrayList<Map<String, Object>>();

    for(ElementType et : elementService.getAllElementTypes()) {
      elementTypeList.add(Map.of("ID",et.getId(), "BEAN_ID",et.getBeanId(),
          "BEAN_VERSION_ID",et.getBeanVersionId(), "NAME",et.getName()));

      for(PropertyType pt : et.getPropertyTypes()) {
        var ptMap = new HashMap<String, Object>();
        ptMap.put("ID", pt.getId());
        ptMap.put("NAME", pt.getName());
        ptMap.put("TYPE", pt.getType());
        ptMap.put("UNIT", Optional.ofNullable(pt.getUnit()).orElse(""));
        propertyTypeList.add(ptMap);

        connectionList.add(Map.of("ELEMENT_TYPE_ID",et.getId(), "PROPERTY_TYPES_ID",pt.getId()));
      }

      for(Map.Entry<String, ElementType> ref : et.getReferences().entrySet()) {
        referencesList.add(Map.of("ELEMENT_TYPE_ID",et.getId(), "REFERENCES_ID",ref.getValue().getId(),
            "REFERENCES_KEY",ref.getKey()));
      }
    }
    // Unify propertyTypes depending on their id
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    List<Map<String, Object>> propertyTypeListUnique = propertyTypeList.stream().filter(
            map -> seen.add(map.get("ID"))).collect(Collectors.toList());

    return Map.of("element_types",elementTypeList, "property_types",propertyTypeListUnique,
        "element_types_property_types",connectionList, "element_type_references",referencesList);
  }

  private Database initDatabaseConnection() throws DatabaseException, SQLException {
    logger.info("Initializing database connection for liquibase migration");
    final Connection connection = jdbcTemplate.getDataSource().getConnection();

    return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
        new JdbcConnection(connection)
    );
  }

  private boolean pullChangelogRepo(CredentialsProvider cred, Path repositorydir) {
    logger.info("Local Git directory: {}", repositorydir.toString());

    Git git = null;
    if (Files.notExists(repositorydir)) {
      logger.info("New repository - clone all migration scripts");
      try {
        Git.cloneRepository()
              .setURI(properties.getMigration().getUrl())
              .setDirectory(repositorydir.toFile())
              .setCredentialsProvider(cred)
              .call();
      } catch (Exception e) {
        logger.error("Failed to clone git repository: {}", e.getMessage());
        return false;
      }
    } else {
      logger.info("Existing repository - pull new migration scripts");
      try {
          git = Git.open(repositorydir.toFile());
          git.pull()
            .setCredentialsProvider(cred);
      } catch (Exception e) {
        logger.warn("Failed to pull new source", e);
        return false;
      } finally {
        if(git!=null) git.close();
      }
    }
    return true;
  }
}
