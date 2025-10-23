package ch.semafor.intens.ws.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import ch.semafor.intens.ws.config.AppProperties;
import ch.semafor.intens.ws.model.swagger.ExQueryParams;
import ch.semafor.intens.ws.service.ComponentsService;
import ch.semafor.intens.ws.service.ProjectsService;
import ch.semafor.intens.ws.service.VariantsService;
import ch.semafor.intens.ws.utils.DateTimeFormatter;
import ch.semafor.intens.ws.utils.IntensWsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Profile("sharding")
@RestController
@RequestMapping("/")
public class ShardController {
  public enum ShardRole {
    MAIN, REPLICA
  }

  public static final Long EXPORT_ID_BASE_NUM = 100000000L;

  public static class Export {

    private List<Owner> owners;
    private List<Map<String, Object>> projects;
    private List<Map<String, Object>> variants;
    private List<Map<String, Object>> components;

    public List<Owner> getOwners() {
      return owners;
    }

    public void setOwners(List<Owner> owners) {
      this.owners = owners;
    }

    public List<Map<String, Object>> getProjects() {
      return projects;
    }

    public void setProjects(List<Map<String, Object>> projects) {
      this.projects = projects;
    }

    public List<Map<String, Object>> getVariants() {
      return variants;
    }

    public void setVariants(List<Map<String, Object>> variants) {
      this.variants = variants;
    }

    public List<Map<String, Object>> getComponents() {
      return components;
    }

    public void setComponents(List<Map<String, Object>> components) {
      this.components = components;
    }
  }

  Logger logger = LoggerFactory.getLogger(ShardController.class);

  private final ShardRole mode;

  private List<String> refValueNames;

  @Autowired
  private AppProperties appProperties;

  @Autowired
  private UserService userService;

  @Autowired
  private ProjectsService projectsService;

  @Autowired
  private VariantsService variantsService;

  @Autowired
  private ComponentsService componentsService;

  @Autowired
  private ElementService elementService;

  public ShardController(
      @Value("${app.sharding-mode:#{''}}") String mode, @Value("${app.refValues:id}") String[] refValueNames) {
    if (mode.length() == 0) {
      throw new IllegalArgumentException(
          "Sharding mode is missing. Cannot use sharding without setting a mode. Please set app.sharding-mode to one of these two values: main, replica");
    }
    if (mode.equalsIgnoreCase("MAIN")) {
      this.mode = ShardRole.MAIN;
    } else if (mode.equalsIgnoreCase("REPLICA")) {
      this.mode = ShardRole.REPLICA;
    } else {
      throw new IllegalArgumentException(
          "Invalid value for app.sharding-mode: \"" + mode + "\". Valid values are: main, replica");
    }
    logger.info("Enabled sharding with mode \"{}\"", mode);
    this.refValueNames = Arrays.asList(refValueNames);
    logger.debug("Setting refValueNames to {}", this.refValueNames);
  }

  @PostMapping(path = "/export")
  @Secured({ "ROLE_USER", "ROLE_ADMIN" })
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
  })
  public Export export(
      @Parameter(schema = @Schema(implementation = ExQueryParams.class)) @RequestParam Map<String, String> queryParams) {
    logger.info("Making shard export of type {}", mode);
    if (mode == ShardRole.MAIN) {
      return exportMain(queryParams);
    }
    return exportShard(queryParams);
  }

  private Export exportMain(Map<String, String> queryParams) {
    var projects = projectsService.find(queryParams);
    List<Map<String, Object>> variants = new ArrayList<>();
    List<Map<String, Object>> components = new ArrayList<>();
    // Keep track of the users to keep the proper ownership in the local database
    var involvedOwners = new HashSet<String>();

    for (var p : projects) {
      var id = (Long) p.get("id");
      var pVariants = variantsService.findByProject(id);
      pVariants.stream().forEach(variant -> variant.putAll(variantsService.findById((Long) variant.get("id"))));
      variants.addAll(pVariants);
      involvedOwners.add((String) p.get("owner"));
    }

    var componentsToGet = new LinkedList<Long>();
    var gotComponents = new HashSet<Long>();

    for (var v : variants) {
      var id = (Long) v.get("id");
      var vComponents = componentsService.findByVariant(id, queryParams);
      vComponents.stream()
          .forEach(component -> componentsToGet.add((Long) component.get("id")));
      involvedOwners.add((String) v.get("owner"));
    }

    // Recursively get all components that are referenced by variants or other
    // components
    while (!componentsToGet.isEmpty()) {
      var id = componentsToGet.pop();
      // If we already have the component, skip
      if (gotComponents.contains(id))
        continue;
      var component = componentsService.findById(id);
      if (component == null)
        continue;
      components.add(component);
      gotComponents.add(id);

      // Find IDs of components that are referenced and add them to the queue
      var newIds = getIds(component);
      componentsToGet.addAll(newIds);
    }

    components = components.stream().distinct().collect(Collectors.toList());
    components.stream().forEach(c -> involvedOwners.add((String) c.get("owner")));

    var owners = userService.findOwners("").stream().filter(o -> involvedOwners.contains(o.getUsername()))
        .collect(Collectors.toList());
    // Remove sensitive info from exported owners
    owners.stream().forEach(o -> o.setPassword(null));
    owners.stream().forEach(o -> o.setTotpSecret(null));

    var export = new Export();
    export.setOwners(owners);
    export.setProjects(projects);
    export.setVariants(variants);
    export.setComponents(components);
    logger.info("Finished export");
    return export;
  }

  private Export exportShard(Map<String, String> queryParams) {
    var elements = elementService.getAllChangedSinceImport();
    // A map of all original IDs and versions mapped to the current local ID
    var idRevMap = elementService.getOriginalIdsBeforeImport();

    // A map of of only the IDs
    var idMap = new HashMap<Long, Long>();
    idRevMap.keySet().stream().forEach(id -> idMap.put(id, idRevMap.get(id).getLeft()));

    var projects = new ArrayList<Map<String, Object>>();
    var variants = new ArrayList<Map<String, Object>>();
    var components = new ArrayList<Map<String, Object>>();
    var idStart = EXPORT_ID_BASE_NUM;

    // Change IDs of new elements to a high number to avoid collission with other
    // elements' original IDs
    for (var e : elements) {
      var id = (Long) e.get("id");
      if (!idMap.containsKey(id)) {
        var newId = idStart++;
        idMap.put(id, newId);
      }
    }

    for (var e : elements) {
      var id = (Long) e.get("id");
      // Set version to the original database's version or 0 if new
      var version = idRevMap.containsKey(id) ? idRevMap.get(id).getRight() : 0;
      e.put("version", version);
      rewriteIds(e, idMap, true);
      e.remove("modcomment");
      switch ((String) e.get("type")) {
        case ProjectsService.PROJECT_TYPE:
          projects.add(e);
          break;

        case ProjectsService.VARIANT_TYPE:
          variants.add(e);
          break;

        default:
          components.add(e);
          break;
      }
    }

    var export = new Export();
    export.setComponents(components);
    export.setVariants(variants);
    export.setProjects(projects);

    return export;
  }

  @PostMapping(path = "/import")
  @Secured({ "ROLE_USER", "ROLE_ADMIN" })
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
  })
  public Object importData(@RequestBody Export data) throws CoreException {
    logger.info("Making shard import of type {}", mode);
    if (mode == ShardRole.MAIN) {
      return importMain(data);
    } else {
      importShard(data);
    }
    return null;
  }

  @Transactional
  public Object importMain(Export data) {
    // Track exceptions that happened while saving to have a list of
    // all conflicts and changes to make a diff.
    var exceptionDatas = new ArrayList<Map<String, Object>>();
    Map<Long, Long> newElementIds = new HashMap<>();

    for (var p : data.getProjects()) {
      var id = p.get("id").toString();
      rewriteIds(p, newElementIds, false);

      try {
        var newId = Long.valueOf(projectsService.save(p).get("id").toString());
        newElementIds.put(Long.valueOf(id), newId);
      } catch (IntensWsException e) {
        if (e.getData() == null) {
          logger.warn("Import failed: {}", e);
        }
        exceptionDatas.add(e.getData());
      }
    }

    for (var v : data.getVariants()) {
      var id = v.get("id").toString();

      rewriteIds(v, newElementIds, false);
      try {
        var newId = Long.valueOf(variantsService.save(v).get("id").toString());
        newElementIds.put(Long.valueOf(id), newId);
      } catch (IntensWsException e) {
        if (e.getData() == null) {
          logger.warn("Import failed: {}", e);
        }
        exceptionDatas.add(e.getData());
      }
    }

    // Import components sorted by how many IDs it references to make sure the stuff
    // that gets references is updated first to get the new IDs in the list
    for (var c : data.getComponents().stream().sorted(
        (i1, i2) -> Integer.valueOf(getIds(i1).size()).compareTo(getIds(i2).size()))
        .collect(Collectors.toList())) {

      var id = c.get("id").toString();

      rewriteIds(c, newElementIds, false);

      try {
        var newId = Long.valueOf(componentsService.save(c).get("id").toString());
        newElementIds.put(Long.valueOf(id), newId);
      } catch (IntensWsException e) {
        if (e.getData() == null) {
          logger.warn("Import failed: {}", e);
        }
        exceptionDatas.add(e.getData());
      }
    }
    if (exceptionDatas.size() > 0) {
      var responseData = new HashMap<String, Object>();
      responseData.put("exceptions", exceptionDatas);
      throw new IntensWsException(responseData, HttpStatus.CONFLICT);
    }

    return "";
  }

  public void importShard(Export data) throws UsernameNotFoundException, CoreException {
    logger.info("project count {}", data.getProjects().size());
    logger.info("variants count {}", data.getVariants().size());
    logger.info("project count {}", data.getComponents().size());
    // Items get a new ID when they get saved so we have to map them to keep the
    // relationships between variants, projects and components
    Map<Long, Long> newElementIds = new HashMap<>();

    for (Owner o : data.getOwners()) {
      // Set the local password of a user to the user's name because locally it
      // doesn't matter and we don't wanna take their actual passwords
      o.setPassword(appProperties.encoder().encode(o.getUsername()));
      userService.saveOwner(o);
      logger.info("Saving owner");
    }

    for (var p : data.getProjects()) {
      var id = Long.valueOf((Integer) p.get("id"));
      logger.info("New project id: {}", id);
      var date = DateTimeFormatter.convert((String) p.get("created"));
      p.put("created", date);
      var version = (Number) p.remove("version");
      // Save the original id and version in the modcomment
      p.put("reason", ElementService.IMPORT_PREFIX + id + "," + version);

      Long newId = (Long) projectsService.create(p).get("id");
      newElementIds.put(id, newId);
    }

    for (var c : data.getComponents().stream().sorted(
        (i1, i2) -> Integer.valueOf(getIds(i1).size()).compareTo(getIds(i2).size()))
        .collect(Collectors.toList())) {
      var id = Long.valueOf((Integer) c.get("id"));
      logger.info("New component id: {}", id);
      var date = DateTimeFormatter.convert((String) c.get("created"));
      c.put("created", date);
      var version = (Number) c.remove("version");
      // Save the original id and version in the modcomment
      c.put("reason", ElementService.IMPORT_PREFIX + id + "," + version);
      rewriteIds(c, newElementIds, true);
      Long newId = (Long) componentsService.create((HashMap<String, Object>) c).get("id");
      newElementIds.put(id, newId);
    }

    for (var v : data.getVariants()) {
      var id = Long.valueOf((Integer) v.get("id"));
      logger.info("New variant id: {}", id);
      var date = DateTimeFormatter.convert((String) v.get("created"));
      v.put("created", date);
      var version = (Number) v.remove("version");
      // Save the original id and version in the modcomment
      v.put("reason", ElementService.IMPORT_PREFIX + id + "," + version);
      rewriteIds(v, newElementIds, true);
      variantsService.create(v);
    }
    logger.info("Finished import");
  }

  /**
   * @param key Key to check
   * @return If the key contains an ID reference to another element
   */
  private boolean isKeyIdRef(String key) {
    var result = key.toLowerCase().endsWith("id") || refValueNames.stream().anyMatch(v -> v.equalsIgnoreCase(key));
    return result;
  }

  /**
   * Recursively search through a component to find all IDs it references
   * 
   * @param element Element to search in
   * @return A list of all IDs referenced in the component
   */
  @SuppressWarnings("unchecked")
  private List<Long> getIds(Object element) {
    var ids = new ArrayList<Long>();
    // If we are on a list, apply function on every entry in the list
    if (element instanceof List) {
      for (var entry : (List<Object>) element) {
        ids.addAll(getIds(entry));
      }
    }

    // If it's a map, first go through the keys that are considred an ID and then
    // apply the function to the rest of the values
    if (element instanceof Map) {
      var _element = (Map<String, Object>) element;

      for (var key : _element.keySet().stream()
          .filter(key -> isKeyIdRef(key))
          .collect(Collectors.toList())) {

        var value = _element.get(key);
        if (!(value instanceof Integer) && !(value instanceof String)) {
          continue;
        }
        if (!NumberUtils.isParsable(value.toString())) {
          continue;
        }
        ids.add(Long.valueOf(value.toString()));
      }

      for (var e : _element.values()) {
        ids.addAll(getIds(e));
      }
    }
    return ids;
  }

  /**
   * Rewrite the IDs of the element to keep the references in tact after a save to
   * a new database assigns a new ID
   * 
   * @param element      Element to start out with
   * @param elementIdMap A map of what IDs should be changed to what
   * @param removeId     Remove the key "id" which is the id of the element
   *                     itself. The id of the element itself should not be
   *                     present when saving a new element, so we can remove it
   *                     here
   */
  @SuppressWarnings("unchecked")
  private void rewriteIds(Object element, Map<Long, Long> elementIdMap, boolean stripUnmapped) {
    // If we are on a list, apply function on every entry in the list
    if (element instanceof ArrayList) {
      for (var entry : (ArrayList<Object>) element) {
        rewriteIds(entry, elementIdMap, stripUnmapped);
      }
    }

    // If it's a map, first go through the keys that are considred an ID and then
    // apply the function to the rest of the values
    if (element instanceof Map) {
      var _element = (Map<String, Object>) element;

      for (var key : _element.keySet().stream().filter(key -> isKeyIdRef(key))
          .collect(Collectors.toList())) {

        var value = _element.get(key);
        if (!(value instanceof Integer) && !(value instanceof Long) && !(value instanceof String)) {
          continue;
        }

        if (!NumberUtils.isParsable(value.toString())) {
          continue;
        }

        var id = Long.valueOf(value.toString());
        var newId = elementIdMap.get(id);

        if (newId != null) {
          _element.put(key, newId);
        } else {
          // If there is no new ID, just map the id to itself to guarantee it's a Long and
          // not a String
          _element.put(key, id);
          if (stripUnmapped) {
            _element.remove(key);
          }
        }
      }

      for (var entry : ((Map<String, Object>) element).values()) {
        rewriteIds(entry, elementIdMap, stripUnmapped);
      }
    }
  }
}
