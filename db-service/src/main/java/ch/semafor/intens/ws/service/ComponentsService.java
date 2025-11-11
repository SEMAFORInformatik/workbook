package ch.semafor.intens.ws.service;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Modification;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.exceptions.ElementTypeCreationException;
import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.exception.TypeCreationException;
import ch.semafor.intens.ws.model.ApprovalState;
import ch.semafor.intens.ws.model.Component;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.model.swagger.ExQueryParams;
import ch.semafor.intens.ws.utils.AccessFilter;
import ch.semafor.intens.ws.utils.ApprovalStateConsistency;
import ch.semafor.intens.ws.utils.ApprovalStateTransition;
import ch.semafor.intens.ws.utils.BaseEntityFactory;
import ch.semafor.intens.ws.utils.IntensWsException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/services/rest")
@Transactional
public class ComponentsService extends BaseServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(ComponentsService.class);

  @Autowired
  ComponentProperties componentProperties;

  @Autowired
  BaseEntityFactory baseEntityFactory;

  @Autowired
  ApprovalStateTransition approvalStateTransition;

	private void checkIfNewOrUpdate(Component comp) {
		  /*
	       *  DO NOT CALL WHEN PARTIAL UPDATE!
	       * useNameRevisionAsIdentifier or
	       * useOwnerNameRevisionAsIdentifier and ApprovalState.EXPERIMENTAL:
	       *    set id (matching type, name and revision)
	       *    if id is found -> status "update"
	       *    if id not found, check if name exists with other revision
	       *    -> status "error" (existing name error)
	       *    -> status "new"

	       * id is identifier: (!useNameRevisionAsIdentifier)
	       * no id -> status "new"
	       * id: check if id matches type, name and revision
	       * id matches: -> status "update"
	       * id does not match: check if creation is allowed
	       * if allowed -> status "new"
	       * if not allowed -> status "error"
	       */
			if (componentProperties.useNameRevisionAsIdentifier){
				// ignore provided id
				comp.setId(null);

				// RestService client (intens) should show a different modify
				// dialog (no New button) when useNameRevisionAsIdentifier is true
				comp.getDbStatus().put("useNameRevisionAsIdentifier", true);
				String owner = null;
				if (componentProperties.useOwnerNameRevisionAsIdentifier){
					owner=comp.getOwner().getUsername();
				}
				// get id that matches type, name and revision and owner eventually (if any)
				List<Map<String, Object>> complist = findByTypeAndNameAndRev(
						comp.getType(), comp.getName(), comp.getRevision(), owner);
	            logger.debug("Size of complist {}", complist.size());
				if (!complist.isEmpty()) {
					// component with same type, name and revision exists -> use its id
					// use first (and only) component
					comp.setId((Long) complist.get(0).get("id"));
					comp.setCreated((Date) complist.get(0).get("created"));
					comp.getDbStatus().put("status", "update");

					// This method only was called when save so set the owner and name from DB
			        comp.setOwner( (String) complist.get(0).get("owner"));
			        comp.setGroup( (String) complist.get(0).get("group"));
			        logger.debug("Set owner: {}", comp.getOwner().getUsername());
			        logger.debug("Set group: {}", comp.getGroup().getName());
					return;
				}
				// status is new

				// check if creation is allowed
				final Long noProjectId = null;
				final Long anyId = null;
				if(getMaxRevision(comp.getType(), comp.getName(), noProjectId, owner, anyId) >= 0) {
					/* component with same type and name but different revision exists
					/* the user wants to create a new component, but the name exists
					/* i.E. loaded component a, rev 2
					/*      changed name to b
					/*      b, rev 2 does not exist, but b, rev 1 exists
					/* -> not allowed
					 *
					 * NOTE: happens only when name has been changed and revision is provided
					 * (Revision should be removed by application in this case)
					 */
					logger.warn("A component with same name but different revision exists");
					comp.getDbStatus().put("status", "error");
					comp.getDbStatus().put("text",
							comp.getType() + " " + comp.getName() +
							" exists already. Please choose a different name.");
					return;
				}
				// no id found, go on
			}

			// id missing (not provided or not found) -> new component
			if (comp.getId() == null) {
				logger.debug("Id not found -> New component");
				comp.getDbStatus().put("status", "new");
				comp.getDbStatus().remove("rev");
				return;
			}

			// id is provided, check if it matches type, name and rev
			/* Now check if type, name and revision match.
			 *
			 * If it matches -> its an UPDATE
			 *
			 * If not -> its a NEW component
			 */
			logger.info("Check id of component {}", comp.getId() );
			final String anyOwner = null;
			// get all components with type, name and revision
			List<Map<String, Object>> complist = findByTypeAndNameAndRev(comp.getType(),
					comp.getName(), comp.getRevision(), anyOwner);
			logger.debug("Found {} components", complist.size());
			for (Map<String, Object> cm : complist) {
				logger.debug("Check if cm.get(id) {} is equal comp.getId() {}",
						cm.get("id"), comp.getId());
				// If the id matches we have the correct component
				if (cm.get("id").equals(comp.getId())) {
					logger.debug("Found a project with the same id -> Update this project");
					comp.setCreated((Date) cm.get("created"));
					comp.getDbStatus().put("status", "update");
	                // This method only was called when save.
	                comp.setOwner( (String) cm.get("owner"));
	                comp.setGroup( (String) cm.get("group"));
	                logger.debug("Set owner: {}", comp.getOwner().getUsername());
	                logger.debug("Set group: {}", comp.getGroup().getName());
					return;
				}
			}

	    	/* id does not match type, name and revision -> NEW component (copy)
	     	*
	     	* We have to check if copying is allowed
	     	*/
			logger.info("id does not match -> new component (copy)");

			// Backup provided owner and group.
			String owner = comp.getOwner().getUsername();
			String group = comp.getGroup().getName();

			// get owner and group of original component (comp.getId())
			// This is important for the AccessFilter to know if this copy is allowed
			Map<String, Object> m = findById(comp.getId());
			if (m != null) {
				comp.setOwner((String) m.get("owner"));
				comp.setGroup((String) m.get("group"));
			}

			// check if creation is allowed (i.E. copy allowed)
			AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);
			Boolean isCreationAllowed = accessFilter.isCreationAllowed(comp);

			// restore owner/group
			comp.setOwner(owner);
			comp.setGroup(group);

			if (isCreationAllowed) {
				logger.debug("creation is allowed");
				comp.getDbStatus().put("status", "new");
				comp.getDbStatus().remove("rev");
				comp.getDbStatus().remove("id");
				comp.setId(null);
				return;
			}

			// creation not allowed
			logger.warn("creation (copy) is not allowed");

			comp.getDbStatus().put("status", "error");
			comp.getDbStatus().put("text",
					"<b>Permission denied</b>. Cannot copy component:<br/>"
							+ comp.getType() + " " + comp + "<br/>");
		}


  /**
   * Get one component by ID
   * @param id Id of the component
   * @return The component
   */
  @GetMapping(path = "/components/{id}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> findById(@PathVariable("id") Long id) {
    return elementService.getElementMap(id);
  }

  /**
   * create new component type
   *
   * @param typedefmap map with property types
   * @return type name of created type
   */
  @Secured({"ROLE_ADMIN", "ROLE_APPADMIN"})
  @PutMapping(path = "/components/type")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public String createType(@RequestBody Map<String, Object> typedefmap) {
    String idName = "id";
    String idVersion = "version";
    String typename = (String) typedefmap.get("type");
    List<Map<String, Object>> typedef = (List<Map<String, Object>>) typedefmap.get("props");
    try {
      ElementType t = elementService.createElementType(typename, typedef, idName, idVersion);
      return t.getName();
    }catch(ElementTypeCreationException ex){
      //logger.error("Type " + typename, ex);
      throw new TypeCreationException(ex.getMessage(), ex);
    }
  }

  /**
   * get list of component types
   *
   * @return list of component types
   */
  @GetMapping(path = "/components/types")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public List<Map<String, Object>> getTypes() {
    List<Map<String, Object>> types = new ArrayList<Map<String, Object>>();
    for (ElementType et : elementService.getAllElementTypes()) {
      Map<String, Object> m = new HashMap<String, Object>();
      m.put("name", et.getName());
      m.put("id", et.getId());
      m.put("propertyTypes", new ArrayList<String>());
      for (PropertyType p : et.getPropertyTypes()) {
        Map<String, Object> mp = new HashMap<String, Object>();
        mp.put("name", p.getName());
        mp.put("type", p.getType());
        if (p.getUnit() != null && !p.getUnit().isEmpty()) {
          mp.put("unit", p.getUnit());
        }
        ((List) m.get("propertyTypes")).add(mp);
      }
      m.put("references", new ArrayList<String>());
      for (String refname : et.getReferences().keySet()) {
        Map<String, Object> mr = new HashMap<String, Object>();
        mr.put("name", refname);
        mr.put("type", et.getElementType(refname).getName());
        ((List) m.get("references")).add(mr);
      }
      types.add(m);
    }
    return types;
  }

  /**
   * search all components via query
   *
   * @param queryParams request parameters
   * @return list of components matching the query
   */
  @GetMapping(path = "/components")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public List<Map<String, Object>> findByQuery(@Parameter(schema = @Schema(implementation = ExQueryParams.class)) @RequestParam Map<String, String>queryParams) {
    return findByType(null, queryParams);
  }

  /**
   * find list of components by type
   *
   * @param type of components
   * @param queryParams request parameters
   * @return list of components of requested type
   */
  @GetMapping(path = "/components/type/{type}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public List<Map<String, Object>> findByType(@PathVariable("type") String type,
                                              @Parameter(schema = @Schema(implementation = ExQueryParams.class)) @RequestParam Map<String, String>queryParams) {
    List<String> minimal = new ArrayList<>(Arrays.asList(
    		"name", "rev", "created", "owner", "ownername", "group", "approval")
    		); // mandatory
	Integer maxChange = -1;
	Integer kindChange = Calendar.MONTH;  // default

	if (queryParams.containsKey("maxChange")) {
		minimal.add("changed");

		String maxChangeStr = String.valueOf(queryParams.get("maxChange"));
		if(maxChangeStr.endsWith("D")) {
			kindChange = Calendar.DAY_OF_YEAR;
			maxChangeStr = maxChangeStr.replaceAll(".$", "");
		}
		else if(maxChangeStr.endsWith("M")) {
			kindChange = Calendar.MONTH;
			maxChangeStr = maxChangeStr.replaceAll(".$", "");
		}
		else if(maxChangeStr.endsWith("Y")) {
			kindChange = Calendar.YEAR;
			maxChangeStr = maxChangeStr.replaceAll(".$", "");
		}

		try {
			maxChange = Integer.parseInt(maxChangeStr);
		}
		catch (NumberFormatException ex){
			logger.warn("Parameter '{}' of maxChange not valid", maxChangeStr);
		}
		queryParams.remove("maxChange");
	}

		Date changedSince = null;

    if( maxChange >= 0 ) {
      Calendar cal = new GregorianCalendar();
      cal.add(kindChange, -maxChange);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      changedSince = cal.getTime();
    }

    List<String> fields = extractFields(queryParams, minimal);

    Map<String, Integer> sortmap = extractSort(queryParams);
    String ownername = extractOwnerName(queryParams);

    // page and pageSize
    int page = extractPage(queryParams);
    int pagesize = extractPageSize(queryParams);

    Map<String, Object> search = extractSearchArgs(queryParams);

    logger.debug("find components of type {}", type);
    List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();

    /**
     * If we have an ID, we can always have access to the component,
     * Do not run AccessFilter
     */
    if (queryParams.containsKey("id")) {
      String id = queryParams.get("id");
      try {
        logger.debug("Type: {}", elementService.getTypeOfElement(Long.parseLong(id)));
        if (!elementService.getTypeOfElement(Long.parseLong(id)).equals(type)) {
          return l;
        }
        Map<String, Object> c = elementService.getElement(Long.parseLong(id), fields);
        if (c != null && !c.isEmpty()) {
          l.add(c);
        }
      } catch (NumberFormatException ex) {
        throw new IntensWsException("Invalid id '" + id + "'" + " for " + type,
            HttpStatus.BAD_REQUEST);
      }

      return l;
    }

    // If we do not have the id, get the component and check if the users is allowed to see the component
    Map<String, Map<String, Object>> childsearch = new HashMap<String, Map<String, Object>>();
    // Return empty list if no element found with this attribute
    if (type != null && !buildSearch(type, queryParams, search, childsearch)) {
      return l;
    }
    // get element
    boolean latestRevision = true;
    l = elementService.findByType(
    		type, ownername, fields, search, childsearch,
        page, pagesize, sortmap, latestRevision, changedSince
    		);

    AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);
    logger.debug("found {} components of type {}", l.size(), type);

    if (!this.hasAdminRole()) {
        l = accessFilter.filterComponents(l);
        logger.debug("--> {} remaining components after filterComponents()", l.size());
    }

	return l;
  }

  /**
   * find components by type and name and revision and owner (if not null)
   *
   * @param type of component
   * @param name of component
   * @param rev of component
   * @param owner of component (any if null)
   * @return list of elements of requested type and name and revision
   */
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public List<Map<String, Object>> findByTypeAndNameAndRev(String type, String name,
		  Integer rev, String owner) {
    Map<String, Object> search = new HashMap<String, Object>();
    search.put("name", new SearchEq<String>(name));

    search.put("rev", new SearchEq<Integer>(rev));
    logger.debug("find type: {} name: {} rev: {}", type, name, search.get("rev"));
    final int page = 0;
    final int pagesize = -1;
    Map<String, Integer> sortmap = new HashMap<String, Integer>();
    sortmap.put("rev", 1);

    final List<String> pnames = Arrays
        .asList("id", "created", "owner", "group", "approval", "version");
    boolean latestRevision = true;
    return elementService
        .findByType(type, owner, pnames, search, null, page, pagesize, sortmap, latestRevision);
  }

  private void setModifications(Component comp) {
    // If we have set an interactive reason and update we must have a modification
    if (comp.getDbStatus().get("status").equals("update") && comp.isInteractiveReason()) {
      comp.getDbStatus().put("status", "modified");
      logger.debug("Modified in interactive reason");
      return;
    }
    Map<String, Object> modifications = elementService.getModifiedProperties(comp.toMap());

    if (modifications.isEmpty()) {
      logger.info("Component not modified");
      comp.getDbStatus().put("status", "notModified");
      comp.getDbStatus().put("version", findById(comp.getId()).get("version"));
      return;
    }
    logger.debug("Modification {}", modifications);
    comp.setModified(modifications);
  }


  private Boolean checkApprovalStateTransition(Component comp) {
    // If status is new -> Always true cause we use default state
    if (comp.getDbStatus().get("status").equals("new")) {
      logger.info("Approval state for new component is default.");
      return true;
    }

    logger.debug("Check Approval state");
    Map<String, Object> m = findById(comp.getId());
    String approvalState = (String) m.get("approval");

    if (approvalState != null) {
      ApprovalState from = ApprovalState.get(approvalState);
      logger.debug("check approval state transition from {} to {}", from, comp.getApprovalState());

      // check: approval state changes from obsolete to something else and that is allowed
      if (comp.check_newRevisionFromObsolete("component", from, comp.getApprovalState())) {
        logger.debug("approval state transition from obsolete to something else is allowed");
        return true;
      }

      if (!approvalStateTransition.isValid(from, comp.getApprovalState())) {
        logger.warn("approval state transition check for {}: failed!", comp.getId());
        comp.getDbStatus().put("status", "error");
        comp.getDbStatus().put("text", "Invalid state transition.");
        /* message text replaced as requested by Alstom (Andrey K.)
            comp.getType() + ": invalid state transition " + from + " --> " + comp
                .getApprovalState()
                + "\nComponent: " + comp.getName() + " (Rev. " + comp.getRevision() + ")"); */
        return false;
      }
    }
    logger.debug("Approval check passed");
    return true;
  }

  private Map<String, Object> checkSingleNonApprovedRevision(Component comp, int maxRevision,
      String reason) {
    // singleNonApprovedRevision:
    // only one revision with approval below approved is allowed
    // new becomes an update of the existing non approved revision if that exists
    if (comp.isNew() &&
        componentProperties.isSingleNonApprovedRevision(comp.getType()) &&
        maxRevision >= 0 // component with name exists
    ) {
      // check if non approved revision exists
    	final String anyOwner = null;
      List<Map<String, Object>> complist = findByTypeAndNameAndRev(comp.getType(), comp.getName(),
          maxRevision, anyOwner);
      if (!complist.isEmpty()) { // must always be true
        // component with same type, name and revision exists -> check if it is not approved
        // isValid returns false if second approval is below first
        logger.debug("max revision {}", maxRevision);
        logger
            .debug("Approval state of max revision component {}", complist.get(0).get("approval"));
        if (!ApprovalStateConsistency.isValid(componentProperties, ApprovalState.Approved,
            ApprovalState.get((String) complist.get(0).get("approval")))) {
          // it is not approved -> update it
          HashMap<String, Object> compToUpdate = (HashMap<String, Object>) comp.toMap();
          compToUpdate.put("id", complist.get(0).get("id"));
          compToUpdate.put("rev", maxRevision);
          compToUpdate.put("version", complist.get(0).get("version")); // avoid up-to-date conflict
          compToUpdate.put("approval",
              complist.get(0).get("approval")); // keep approval of updated component
          compToUpdate.put("type", comp.getType());
          if (reason != null) {
            String key = "reason";
            if (comp.isInteractiveReason()) {
              key = "interactiveReason";
            }
            compToUpdate.put(key, reason);
          }
          return compToUpdate;
        }
      }
    }
    return null;
  }

  /**
   * save component
   *
   * @param compdata map with property values
   * @return saved component
   */
  @PutMapping(path = "/components")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> save(@RequestBody Map<String, Object> compdata) {
    Component comp = null;

      if (logger.isDebugEnabled() && compdata.containsKey("rev")) {
          logger.debug("save: rev is {}", compdata.get("rev"));
      }

    // Status: unknown

    // default owner just in case
    Owner owner = getOwner();

    comp = baseEntityFactory.createComponent(compdata, owner);
    // return if create Component gives an error
    if (comp.getDbStatus().get("status").equals("error")) {
      logger.warn("create component failed -> status: {}", comp.getDbStatus());
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }
    // return if not modified
    if (comp.getDbStatus().get("status").equals("notModified")) {
      logger.debug("return empty Map <- new Component");
      return Collections.emptyMap();
    }
    // status: unknown

    checkIfNewOrUpdate(comp); // status is new, update or error

    String reason = comp.checkReason(compdata);      // If no reason was send the reason is null

    logger.debug("Status: {}", comp.getDbStatus().get("status"));

    // check if checkIfNewOrUpdate gives an error
    if (comp.getDbStatus().get("status").equals("error")) {
      logger.warn("create component failed -> status: {}", comp.getDbStatus());
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }

    // Approval State transition Check if not interactive
    if (!comp.isInteractiveReason() && !checkApprovalStateTransition(comp)) {
      logger.warn("Check component failed -> status: {}", comp.getDbStatus());
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }

    // parameters of type DATE must be converted before checking for modifications and before saving
    // otherwise, they are said to be modified (string != date)
    fixDate(comp.toMap(), comp.getType());

    // Set modification in both mode interactiveReason and reason.
    // -> In interactiveReason we have to set the status: update -> status: modified
    if (comp.getDbStatus().get("status").equals("update")) {
      logger.debug("Update of component");
      setModifications(comp);
    }

    // Check if modify. If not we can return
    if (comp.getDbStatus().get("status").equals("notModified")) {
      if (comp.getId() != null) {
        logger.info("Load component from id");
        return findById(comp.getId());
      }
      logger.debug("return empty Map <- new Component");
      return Collections.emptyMap();
    }
    final Long noProjectId = null;
    final String anyOwner = null;
    final Long anyId = null;
    int maxRevision = -1;  // new, create revision 0
    if(!comp.isNew() || !componentProperties.useNameRevisionAsIdentifier){  // not new, create next revision
        maxRevision = getMaxRevision(comp.getType(), comp.getName(), noProjectId, anyOwner, anyId);
        comp.applyRules(false, elementService, new User(owner), kieSession, maxRevision, "modify");
    }
    // Check up-to-date conflict
    try {
      checkVersion(comp, comp.getDbStatus(), comp.getType());
    } catch (UsernameNotFoundException e) {
      throw new IntensWsException(comp.getDbStatus(), HttpStatus.BAD_REQUEST);
    }

    if (comp.getDbStatus().get("status").equals("error")) {
      logger.warn("Check component failed -> status: {}", comp.getDbStatus());
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }

    // check single non approved revision
    Map<String, Object> compToUpdate = checkSingleNonApprovedRevision(comp, maxRevision, reason);
    if (compToUpdate != null) { // save maxRevision component
      return save((HashMap<String, Object>) compToUpdate);
    }

    // No reason
    if (reason == null) {
      logger.debug("No interactive reason or reason found status: {}", comp.getDbStatus());
      throw new IntensWsException(comp.getDbStatus(), HttpStatus.CONFLICT);
    }

    // Up to date conflict
    if (comp.isUpToDateConflict()) {
      logger.debug("Up-to-date conflict status: {}", comp.getDbStatus());
      throw new IntensWsException(comp.getDbStatus(), HttpStatus.CONFLICT);
    }

    if (comp.isNew()) {
      comp.setOwner(owner.getUsername());
      comp.setGroup(owner.getActiveGroup().getName());
      logger.debug("New Component, set User:{}, Group: {}",
          owner.getUsername(), owner.getActiveGroup().getName());
      comp.setCreated(new Date());
      comp.setNextRevision(maxRevision);
      comp.incRevision();
      // TODO: ApprovalState.Default
      // use property component.defaultApprovalStateRev0 for revision 0, if available
      comp.setApprovalState(componentProperties.getDefaultApprovalState(
    		  "component", comp.getRevision() == 0));
    }

    try {
      Map<String, Object> savedData =
          elementService.save(comp.toMap(), comp.getType(), getUser(), reason);
      logger.debug("after save: {}", savedData.toString());
      return savedData;

    } catch (CoreException e) {
      throw new IntensWsException(
          "<b>" + e.getMessage() + "</b><br/>" + comp.getType() + " " + comp + "<br/>",
          HttpStatus.BAD_REQUEST);
    } catch (IntensWsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Fatal error: {}", e);
      throw new IntensWsException("Fatal error", // "Unknown format:
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  /**
   * check status and modified properties
   *
   * @param compdata component data values
   * @return status code and map with modified properties (if any)
   */
  @PutMapping(path = "/components/check")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> checkStatus(@RequestBody HashMap<String, Object> compdata) {
    Component comp = baseEntityFactory.createComponent(compdata, getOwner());
    // check if createComponent gives an error
    if (comp.getDbStatus().get("status").equals("error") ||
        comp.getDbStatus().get("status").equals("notModified")) {
      logger.warn("create component failed -> status: {}", comp.getDbStatus());
      return (HashMap<String, Object>) comp.getDbStatus();
    }
    // status: unknown

    checkIfNewOrUpdate(comp);

    if (!comp.getDbStatus().get("status").equals("new")) {
      comp.getDbStatus().put("group", comp.getGroup().getName());
      comp.getDbStatus().put("username", getOwner().getFullName());
    }

    if (comp.getDbStatus().get("status").equals("error")) {
      return (HashMap<String, Object>) comp.getDbStatus();
    }

    if (!checkApprovalStateTransition(comp)) {
      return (HashMap<String, Object>) comp.getDbStatus();
    }

    if (comp.getDbStatus().get("status").equals("update")) {
      logger.debug("Component is update");

      // parameters of type DATE must be converted before checking for modifications
      // otherwise, they are said to be modified (string != date)
      fixDate(comp.toMap(), comp.getType());
      setModifications(comp);

      if (comp.getDbStatus().get("status").equals("modified")) {
        final Long noProjectId = null;
        final String anyOwner = null;
        final Long anyId = null;
        final int maxRevision = getMaxRevision(comp.getType(), comp.getName(), noProjectId, anyOwner, anyId);
        comp.applyRules(false, elementService,
            new User(getOwner()), kieSession, maxRevision,
            "modify");

        // Check up-to-date conflict
        try {
          checkVersion(comp, comp.getDbStatus(), comp.getType());
        } catch (UsernameNotFoundException e) {
          throw new IntensWsException(comp.getDbStatus(), HttpStatus.BAD_REQUEST);
        }

        // check single non approved revision
        Map<String, Object> compToUpdate = checkSingleNonApprovedRevision(comp, maxRevision, null);
        if (compToUpdate != null) { // checkStatus of maxRevision component
          return checkStatus((HashMap<String, Object>) compToUpdate);
        }

      }
    }
    return (HashMap<String, Object>) comp.getDbStatus();
  }

  /**
   * import a component (keeps creation date)
   * @param compdata component data
   * @return import component
   **/
  @Secured("ROLE_ADMIN")
  @PutMapping(path = "/components/import")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> create(@RequestBody Map<String, Object> compdata) {
    Component comp = null;
    String ownername = null;
    try {
      // missing owner -> return BAD_REQUEST
      if (!compdata.containsKey("owner") || ((String) compdata.get("owner")).isEmpty()) {
        logger.warn("No owner found");
        throw new IntensWsException("No owner found", HttpStatus.BAD_REQUEST);
      }
      ownername = (String) compdata.get("owner");
      Owner owner = userService.findOwnerByUsername(ownername);
      comp = baseEntityFactory.createComponent(compdata, owner);

      comp.setId(null);
      String reason = compdata.containsKey("reason") ? (String) compdata.get("reason") : "imported";
      return elementService.save(comp.toMap(), comp.getType(), owner.getUsername(), reason);

    } catch (IllegalArgumentException e) {
      throw new IntensWsException(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (CoreException e) { // up-to-date conflict
      throw new IntensWsException(
          "<b>" + e.getMessage() + "</b><br/>" + comp.getType() + " " + comp + "<br/>",
          HttpStatus.BAD_REQUEST);
    } catch (ClassCastException e) {
      throw new IntensWsException(e, // "Unknown format:
          // "+compdata.toString(),
          HttpStatus.BAD_REQUEST);
    } catch (UsernameNotFoundException e) {
      throw new IntensWsException("Owner not found: " + ownername, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * find modifications of component
   *
   * @param id of component
   * @param queryParams query parameters
   * @return list of modifications
   */
  @GetMapping(path = "/components/{id}/modifications")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public List<Map<String, Object>> findModifications(@PathVariable("id") Long id,
      @RequestParam Map<String, String> queryParams) {

    // page and pageSize
    int page = extractPage(queryParams);
    int pagesize = extractPageSize(queryParams);

    List<Map<String, Object>> modlist = new ArrayList<Map<String, Object>>();
    for (Modification m : elementService
        .getModifications(id, page, pagesize, Sort.Direction.DESC)) {
      HashMap<String, Object> h = new HashMap<String, Object>();
      h.put("user", m.getUser().getFullName());
      // h.put("user", m.getUser().getUsername());
      h.put("changed", m.getTimestamp());
      h.put("mod_id", m.getRevision());
      h.put("reason", m.getComment());
      modlist.add(h);
      // System.out.println( m.toString());
    }
    return modlist;
  }

  /**
   * find modification of component by id
   *
   * @param id of component
   * @param mod_id modification id
   * @return modification
   */
  @GetMapping(path = "/components/{id}/modifications/{mod_id}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> findByModification(@PathVariable("id") Long id,
      @PathVariable("mod_id") Long mod_id) {
    try {
      return elementService.getElement(id, mod_id);
    } catch (CoreException e) {
      new IntensWsException(e.getMessage() + ":\n" + "Modification " + mod_id + " of Element " + id,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return null;
  }

  /**
   * find all components in variant
   *
   * @param varId of variant
   * @param queryParams query parameters
   * @return list of components in variant
   */
  @GetMapping(path = "/components/variant/{id}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public List<Map<String, Object>> findByVariant(@PathVariable("id") Long varId,
      @RequestParam Map<String, String> queryParams) {
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
      if (varId == null) {
          return result;
      }
    List<String> minimal = Arrays
        .asList("name", "rev", "created", "owner", "ownername", "group", "approval",
            "type"); // mandatory
    // fields
    List<String> fields = extractFields(queryParams, minimal);

    Map<String, Object> variant = elementService.getElementMap(varId);
    logger.debug("variant {}", variant);
    String varcomp = null; // all
    Set<Number> compids = new HashSet<Number>();
    for (String refname : getVarCompNames(varcomp)) {
      Object r = variant.get(refname);
      if (r != null) {
        if (r instanceof Map) {
          Number n = (Number) ((Map) r).get("compId");
          if (n != null && !compids.contains(n.longValue())) {
            Long id = n.longValue();
            result.add(elementService.getElement(id, fields));
            compids.add(id);
            logger.debug(" compid {}", id);
          }
        } else if (r instanceof Collection) {
          for (Map m : (Collection<Map>) r) {
            Number n = null;
            try {
              n = (Number) m.get("compId");
            } catch (Exception e) {
              if (m.get("compId") instanceof List) {
                n = ((ArrayList<Number>) m.get("compId")).get(0);
                logger.debug("Number: {}", n);
              } else {
                logger.error("Could not convert compId in Number {}", e.getMessage());
              }
            }
            logger.debug(" map {} n {}", m, n);
            if (n != null && !compids.contains(n.longValue())) {
              Long id = n.longValue();
              result.add(elementService.getElement(id, fields));
              compids.add(id);
            }
          }
        } else {
          logger.warn("unknown type of r {}", r.getClass());
        }
      }
    }
    return result;
  }

  private Component getCheckedComponent(Long id, String mode) {
    // mode: delete (delete) or modify (rename)
    if (id == null) {
      throw new IllegalArgumentException("Component Id is null");
    }
    // check owner
    Map<String, Object> m = elementService.getElementMap(id);
    if (m == null) {
      throw new IllegalArgumentException("Component " + id + " not found");
    }

    // Set type
    String type = elementService.getTypeOfElement(id);
    m.put("type", type);
    Component comp = baseEntityFactory.createComponent(m, null);

    // apply rules
    logger.debug("apply rules for {}", comp.getId());
    final Long noProjectId = null;
    final String anyOwner = null;
    final Long anyId = null;
    comp.applyRules(false, elementService, new User(getOwner()), kieSession,
        getMaxRevision(comp.getType(), comp.getName(), noProjectId, anyOwner, anyId), mode);

    logger.debug("Status after apply: {}", comp.getDbStatus().get("status"));
    // Check if apply rules throw an error
    if (!comp.isValid()) {
      logger.warn("apply rules failed: {}", comp.getError());
      throw new IllegalArgumentException("Permission denied : you are not allowed to " + mode
          + " " + comp.getType() + " " + comp.getName() + ", Rev." + comp.getRevision());
    }

    logger.debug("apply rules passed!");

    // check approval state
    if (comp.getApprovalState() != ApprovalState.InPreparation
        && comp.getApprovalState() != ApprovalState.Obsolete
        && comp.getApprovalState() != ApprovalState.Experimental) {
      throw new IllegalArgumentException(
          "Permission denied for approval state " + comp.getApprovalState());
    }
    return comp;
  }

  /**
   * rename component
   *
   * @param id of component
   * @param newname new name
   * TODO: check if this method should be replaced by update.
   * @return component
   */
  @PutMapping(path = "/components/{id}/rename")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> rename(@PathVariable("id") Long id, @RequestBody String newname, @RequestParam(required = false, defaultValue = "rename") String reason) {
    logger.debug("Id {}, newname {}", id, newname);
    try {
      Component comp = getCheckedComponent(id, "modify");
      logger.debug("component {}", comp);
      comp.setId(id);
      // strip leading and trailing quotation chars
      newname = stripQuotationChars(newname);
      comp.setName(newname);
      // return id, name, rev
      Map<String, Object> emap = elementService
          .save(comp.toMap(), comp.getType(), getUser(), reason);
      Map<String, Object> rmap = new HashMap<String, Object>();
      for (String key : new String[]{"id", "name", "rev", "version"}) {
        if (emap.containsKey(key)) {
          rmap.put(key, emap.get(key));
        }
      }
      return rmap;

    } catch (CoreException | IllegalArgumentException e) {
      throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
    } catch (UsernameNotFoundException e) {
      throw new IntensWsException("Owner not found: " + getUser(), HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * approve component
   *
   * @param id of component
   * @param approval name
   * @return component
   */
  @PutMapping(path = "/components/{id}/approve")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> approve(@PathVariable("id") Long id, @RequestBody String approval) {
    logger.debug("Id {}, approval {}", id, approval);
    try {
      Map<String, Object> m = elementService.getElementMap(id);
      if (m == null) {
        throw new IntensWsException("Component " + id + " not found", HttpStatus.BAD_REQUEST);
      }

      // Set type
      String type = elementService.getTypeOfElement(id);
      m.put("type", type);
      Component comp = baseEntityFactory.createComponent(m, null);
      logger.debug("component {}", comp);
      comp.setId(id);
      // strip leading and trailing quotation chars
      approval = stripQuotationChars(approval);
      var state = ApprovalState.get(approval);
      if(!approvalStateTransition.isValid(comp.getApprovalState(), state)) {
        throw new IntensWsException("Invalid state transition.", HttpStatus.BAD_REQUEST);
      }
      comp.setApprovalState(state);
      // return id, name, rev
      Map<String, Object> emap = elementService.save(comp.toMap(), comp.getType(), getUser(),
          "approve " + approval);
      Map<String, Object> rmap = new HashMap<String, Object>();
      for (String key : new String[]{"id", "name", "rev", "version"}) {
        if (emap.containsKey(key)) {
          rmap.put(key, emap.get(key));
        }
      }
      return rmap;

    } catch (CoreException | IllegalArgumentException e) {
      throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
    } catch (UsernameNotFoundException e) {
      throw new IntensWsException("Owner not found: " + getUser(), HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * delete component
   *
   * @param id component
   */
  @DeleteMapping(path = "/components/{id}")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public void delete(@PathVariable Long id) {
    /**
     * Delete is a special function an we can only delete a component if
     * this component was never attached to an (now) existing Variant!
     *
     * So if you want to delete a component/model from a variant. And the
     * variant still exists, you can not delete it! Cause we must have the
     * possibility to go back to that state where the component still exists
     * / was connected to the variant!
     *
     * So delete is possible if you create a component and never attached to
     * a variant, or the variant who the component was attached is also
     * deleted!
     */
    try {
      Component comp = getCheckedComponent(id, "delete");
      logger.debug("Found Component: {}", comp);

      // founded variants are put in this list
      List<Map<String, Object>> vlist = new ArrayList<Map<String, Object>>();

      // Prepare variables for the findByType <- All null but with this,
      // we know which arguments are
      // passed the method instead of null, null, null
      Map<String, Object> search = null;
      final String anyOwner = null;
      final String anyComp = null;
      final List<String> noPnames = null;

      // We are looking for a compId in variant
      Map<String, Object> m = new HashMap<String, Object>();
      m.put("compId", new SearchEq<Integer>(id.intValue()));

      // Return all references that belongs to a variant (z.B: model)
      for (String refname : getVarCompNames(anyComp)) {
        // varcompsearch = childsearch
        Map<String, Map<String, Object>> varcompsearch = new HashMap<String, Map<String, Object>>();
        varcompsearch.put(refname, m);

        logger.debug("Search variants ref = {}", refname);

        // IMPORTANT. set the latest revision to false! We have to check
        // if the component was once attached
        // to the variant in the whole lifetime of the variant!
        boolean latestRevision = false;

        vlist.addAll(
            elementService.findByType(VariantsService.VARIANT_TYPE, anyOwner, noPnames, search,
                varcompsearch, // childsearch
                0, 0, null, latestRevision));

        logger.debug("Size of vlist {}", vlist.size());

        // Can this ever happen? Does the search will be filled in
        // findByType?
        if (search != null) {
          logger.debug("search {}", search.toString());
        }
        // Debug
        logger.debug("varcompsearch {}", varcompsearch);
        logger.debug("variants size {}", vlist.size());
      }
      if (vlist.size() > 0) {
        if (logger.isDebugEnabled()) {
          for (Map<String, Object> element : vlist) {
            for (Object value : element.keySet()) {
              logger.debug("variant[{}]: {}", value, element.get(value));
            }

          }
        }
        String variants = "";
        for (Map<String, Object> element : vlist) {
          for (Object value : element.keySet()) {
            logger.debug("variant[{}]: {}", value, element.get(value));

          }
          variants += "ID: " + element.get("id") + " Name: " + element.get("name") + ", ";

        }
        throw new IllegalArgumentException(
            "Component " + comp.getName() + "(Rev. " + comp.getRevision()
                + ") has " + vlist.size() + " connection/s to variants: " + variants);
      }
      elementService.deleteElement(id);
    } catch (CoreException e) {
      // should not happen, we checked everything
      logger.error("Unexpected exception during delete", e);
    } catch (IllegalArgumentException ex) {
      throw new IntensWsException(ex, HttpStatus.BAD_REQUEST);
    }
  }


  /**
   * update component
   *
   * @param data used for update
   * @param id of component
   * @return component
   */
  @PutMapping(path = "/components/{id}")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> update(@RequestBody HashMap<String, Object> data,
      @PathVariable("id") Long id) {

    String type = null;
    logger.debug("Update Component with id {}", id);

    if (!data.containsKey("type") || ((String) data.get("type")).isEmpty()) {
      logger.debug("Type is missing");
      throw new IntensWsException("Type missing:", HttpStatus.BAD_REQUEST);
    }
    type = (String) data.get("type");

    logger.debug("Start partial update of component with id {}", id);
    Map<String, Object> component_from_db = findById(id);

    if (component_from_db == null) {
      logger.error("FAIL step 1 of 8: No component found with id: {}", id);
      throw new IntensWsException("Component with id: " + id + " not found",
          HttpStatus.BAD_REQUEST);
    }

    logger.debug("Component loaded: {}", component_from_db);
    // Return data direct if data is empty
      if (data.isEmpty()) {
          return component_from_db;
      }

    logger.info("Update component");

    // Check if name exists
    String origName = null;
    if (componentProperties.useNameRevisionAsIdentifier) {
    	// TODO: reduce complexity i.e. by calling functions
        boolean checkIfNameExists = false;
        String name = (String) component_from_db.get("name");   // existing name
        if (data.containsKey("name")) {
            data.put("name", stripQuotationChars((String) data.get("name")));
            if (!component_from_db.get("name").equals(data.get("name"))) {
            	origName = name;  // needed to rename other components
                logger.debug("Name is changed {} -> {}", component_from_db.get("name"), data.get("name"));
                name = (String) data.get("name");  // new name
                checkIfNameExists = true;
            }
        }
        if (componentProperties.useOwnerNameRevisionAsIdentifier &&  // same name allowed (experimental, rev 0)
            !checkIfNameExists &&  // name did not change
            data.containsKey("approval") &&  // new approval provided
            !component_from_db.get("approval").equals(data.get("approval")) &&  // approval changed
            component_from_db.get("approval").equals(ApprovalState.Experimental.getName())  // approval was experimental
            ) {
            logger.debug("Approval is changed {} -> {}",
                         component_from_db.get("approval"), data.get("approval"));
            checkIfNameExists = true;
        }

        if(checkIfNameExists) {
            final Long noProjectId = null;
            String owner = null;
            if (componentProperties.useOwnerNameRevisionAsIdentifier) {
                owner = (String) component_from_db.get("owner");
            }
            if (getMaxRevision(type, name, noProjectId, owner, id) >= 0) {
                logger.warn("Name already exists: Not allowed");
                // modified as requested by Alstom (Andrey K.): removed name
                throw new IntensWsException("A component with this name already exists.",
                                            HttpStatus.BAD_REQUEST);
            }
        }
    }  // finish checking name

    // parameters of type DATE must be converted before checking for modifications and before saving
    // otherwise, they are said to be modified (string != date)
    // Only data needs to be checked
    fixDate(data, type);

    HashMap<String, Object> updated_comp = (HashMap<String, Object>) updateElementValues(data, id);
    
    Component comp = baseEntityFactory.createComponent(updated_comp, getOwner());
    // check if createComponent gives an error
    if (comp.getDbStatus().get("status").equals("error")) {
      logger.warn("create component failed -> status: {}", comp.getDbStatus());
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }

    comp.getDbStatus().put("status", "update");

    logger.debug("Created component: {}", comp.toMap().toString());

    String reason = comp.checkReason(data);      // If no reason was sent the reason is null

    if (!comp.isInteractiveReason() && !checkApprovalStateTransition(comp)) {
      logger.warn("Approval State check failed: {}", comp.getDbStatus().get("text"));
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }
    logger.debug("Approval state check passed or not neccessary");

    setModifications(comp);

    if (comp.getDbStatus().get("status").equals("notModified")) {
      logger.info("Component not modified");
      return component_from_db;
    }
    final Long noProjectId = null;
    final String anyOwner = null;
    final Long anyId = null;
    final int maxRevision = getMaxRevision(comp.getType(), comp.getName(), noProjectId, anyOwner, anyId);
    if(!this.hasAdminRole()){  // attention: this allows an admin to modify an approved component
        comp.applyRules(true, elementService, new User(getOwner()), kieSession, maxRevision, "modify");
    }
    // Check up-to-date conflict
    try {
      checkVersion(comp, comp.getDbStatus(), comp.getType());
    } catch (UsernameNotFoundException e) {
      throw new IntensWsException(comp.getDbStatus(), HttpStatus.BAD_REQUEST);
    }

    if (comp.getDbStatus().get("status").equals("error")) {
      logger.warn("Check component failed -> status: {}", comp.getDbStatus());
      throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
    }

    // check single non approved revision
    Map<String, Object> compToUpdate = checkSingleNonApprovedRevision(comp, maxRevision, reason);
    if (compToUpdate != null) { // update maxRevision component
      return update(data, (Long) compToUpdate.get("id"));
    }

    // No reason
    if (reason == null) {
      logger.debug("No interactive reason or reason found status: {}", comp.getDbStatus());
      throw new IntensWsException(comp.getDbStatus(), HttpStatus.CONFLICT);
    }

    // Up to date conflict
    if (comp.isUpToDateConflict()) {
      logger.debug("Up-to-date conflict status: {}", comp.getDbStatus());
      throw new IntensWsException(comp.getDbStatus(), HttpStatus.CONFLICT);
    }

    if (comp.isNew()) {
      logger.debug("New Component");
      comp.setCreated(new Date());
      comp.setNextRevision(getMaxRevision(comp.getType(), comp.getName(), noProjectId, anyOwner, anyId));
      comp.incRevision();
      // TODO: ApprovalState.Default
      comp.setApprovalState(componentProperties.getDefaultApprovalState(
    		  "component", comp.getRevision() == 0));
    }

    try {
      Map<String, Object> savedData = elementService
          .save(comp.toMap(), comp.getType(), getUser(), reason);
      logger.debug("after save: {}", savedData.toString());
      if(origName == null){
    	  return savedData;
      }
      // TODO: beware of new comp --> id==null, NPE
      logger.debug("rename other revisions of this component");
      // rename other revisions of this component:
      List<String> pnames = new ArrayList<>(); // we don't need anything except id 
      boolean latestRevision = true;
      String name = comp.getName();
      Map<String, Object> searchargs = new HashMap<String, Object>();
      searchargs.put("name", new SearchEq<String>(origName));
      if(componentProperties.useOwnerNameRevisionAsIdentifier) {
          SearchEq<String> searchExp = new SearchEq<>(
        		  ApprovalState.Experimental.getName());
          searchargs.put("_not_approval", searchExp);
      }

      for (Map<String, Object> m : elementService.findByType(type,
              anyOwner, pnames, searchargs, null, 0, 0, null, latestRevision)) {
    	  if(!comp.getId().equals((Long)m.get("id"))) {
    		  logger.debug("rename {} id {} name {}", 
    				  type, m.get("id"), name);
    		  m.put("name", (Object)name);
    		  elementService.save(
        		  m, comp.getType(), (String)m.get("owner"), "renamed to " + name);
    	  }
      }
      return savedData;

    } catch (CoreException e) {
      throw new IntensWsException(
          "<b>" + e.getMessage() + "</b><br/>" + comp.getType() + " " + comp + "<br/>",
          HttpStatus.BAD_REQUEST);
    } catch (IntensWsException e) {
      throw e;
    } catch (Exception e) {
      Writer writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);
      e.printStackTrace(printWriter);
      e.printStackTrace();
      throw new IntensWsException(writer.toString(), // "Unknown format:
          // "+compdata.toString(),
          HttpStatus.BAD_REQUEST);
    }
  }

}
