package ch.semafor.intens.ws.service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.internal.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.ApprovalState;
import ch.semafor.intens.ws.model.Project;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.model.Variant;
import ch.semafor.intens.ws.model.swagger.ExCheckStatus;
import ch.semafor.intens.ws.model.swagger.ExNewProject;
import ch.semafor.intens.ws.model.swagger.ExProject;
import ch.semafor.intens.ws.model.swagger.ExQueryParams;
import ch.semafor.intens.ws.utils.AccessFilter;
import ch.semafor.intens.ws.utils.BaseEntityFactory;
import ch.semafor.intens.ws.utils.IntensWsException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/services/rest")
@Transactional
public class ProjectsService extends BaseServiceImpl {
	private static final Logger logger = LoggerFactory.getLogger(ProjectsService.class);

	@Autowired
	ComponentProperties componentProperties;

	@Autowired
	BaseEntityFactory baseEntityFactory;

	@Autowired
	private Environment env;

	/**
   * Get a project by id
   *
	 * @param id id of the project
	 * @return The project with the given id
	 */
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	@GetMapping(path = "/projects/{id}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExProject.class))
      })
  })
	public Map<String, Object> findById(@PathVariable("id") Long id) {
		logger.debug("find element by id {}", id);
		return elementService.getElementMap(id);
	}

	/**
   * Get a list of projects based on a query
   *
	 * @param queryParams query parameters
	 * @return Projects matching the query
	 */
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	@GetMapping(path="/projects")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", array = @ArraySchema(schema =
          @Schema(implementation = ExProject.class)))
      })
  })
	public List<Map<String, Object>> find(
    @Parameter(schema = @Schema(implementation = ExQueryParams.class))
    @RequestParam Map<String, String> queryParams
  ) {
		List<String> minimal = new ArrayList<>(Arrays.asList(
				"name", "created", "desc", "owner", "ownername", "group", "status")
				); // mandatory fields
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

		List<String> fields = extractFields(queryParams, minimal);

		String ownername = extractOwnerName(queryParams);
		Map<String, Integer> sortmap = extractSort(queryParams);

		// page and pageSize
		int page = extractPage(queryParams);
		int pagesize = extractPageSize(queryParams);

		Map<String, Object> search = extractSearchArgs(queryParams);

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

		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		if (queryParams.containsKey("id")) {
			String id = queryParams.get("id");
			logger.debug("find project by id(s) {}", id);
			try {
				Map<String, Object> p = elementService.getElement(Long.valueOf(id), fields);
				if (p != null && !p.isEmpty()) {
					l.add(p);
				}
			} catch( NumberFormatException ex ){
				throw new IntensWsException( "Invalid id '"+id+"'" + " for project",
						HttpStatus.BAD_REQUEST);
			}
		} else {
			Map<String, Map<String, Object>> childsearch = new HashMap<String, Map<String, Object>>();
			logger.debug("find {} {} {}", PROJECT_TYPE,
					ownername, search);

			if (!buildSearch(PROJECT_TYPE, queryParams, search, childsearch)) {
				return l;
			}
			boolean latestRevision = true;
			l = elementService.findByType(PROJECT_TYPE, ownername, fields,
					search, childsearch, page, pagesize, sortmap, latestRevision, changedSince);
		}

		AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);
		logger.debug("found {} projects", l.size());

		if (!this.hasAdminRole()) {
			l = accessFilter.filterProjects(l);
			logger.debug("--> {} remaining projects after filterProjects()", l.size());
		}

		return l;
	}

	/**
   * Get projects using a specific component
   *
	 * @param compid Id of the component to use
	 * @param varcomp optional variant to search under for projects
	 * @return List of found projects
	 */
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	@GetMapping(path = "/projects/component/{id}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ExProject.class)))
      })
  })
	public List<Map<String, Object>> findByComponent(@PathVariable("id") Long compid,
													 @RequestParam("varcomp") String varcomp) {
		List<Map<String, Object>> plist = new ArrayList<Map<String, Object>>();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("compId", new SearchEq<Integer>(compid.intValue()));

		logger.debug("Search variants with compId = {}", compid);

		String owner = null; // / any owner

		AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);

		Set<Number> projids = new HashSet<Number>();
		for (String refname : getVarCompNames(varcomp)) {
			Map<String, Map<String, Object>> childsearch = new HashMap<String, Map<String, Object>>();
			childsearch.put(refname, m);
			boolean latestRevision = true;
			for (Map<String, Object> v : elementService.findByType(
					VariantsService.VARIANT_TYPE, owner,
					Arrays.asList("projectId"), null,
					childsearch, 0, 0, null, latestRevision)) {
				Number n = (Number) v.get("projectId");
				if (!projids.contains(n)) { // we don't want the same project
											// twice
					logger.debug("Found project id {} for variant {}", n,
							refname);
					plist.add(findById(n.longValue()));
					projids.add(n);
				}
			}
		}
		return accessFilter.filterProjects(plist);
	}

	/**
   * Add a new project
   *
	 * @param data
	 * @return the new project
	 */
	@Secured("ROLE_USER")
	@Transactional
	@PutMapping(path = "/projects", consumes = "application/json", produces = "application/json")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExProject.class))
      })
  })
	public Map<String, Object> save(@Parameter(
			schema = @Schema(implementation =  ExNewProject.class)) @RequestBody Map<String, Object> data) {
		// Remove the provided owner and group
		data.remove("owner");
		data.remove("group");

		HashMap<String, Object> status = new HashMap<String, Object>();
		try {
			String reason = "";
			if (data.containsKey("interactiveReason")) {
				reason = (String) data.get("interactiveReason");
				data.remove("interactiveReason");
			} else { // reason missing or provided by application -> check status
				// reason provided by application -> use it
				boolean reasonMissing = true;
				if (data.containsKey("reason")) {
					reason = (String) data.get("reason");
					data.remove("reason");
					reasonMissing = false;
				}
				logger.debug("No interactive reason found -> check");
				status = checkStatus(data);
				if (status.get("status").equals("notModified")) {
					if (data.containsKey("name") && status.containsKey("id")) {
						return findById(((Number) status.get("id")).longValue());
					}
					return Collections.emptyMap();
				}
				if (status.get("status").equals("error")) {
					throw new IntensWsException((String) status.get("text"),
							HttpStatus.BAD_REQUEST);
				}
				// up-to-date conflict or missing reason -> return status
				// (conflict)
				if (status.get("status").equals("upToDateConflict")
						|| reasonMissing) {
					throw new IntensWsException(status,
							HttpStatus.CONFLICT);
				}
			}
			if (!data.containsKey("id")) { // when creating a new project
				// check name
				List<Map<String, Object>> prolist = findByName((String) data.get("name"));
				if (!prolist.isEmpty()) {
					logger.error("Name {} already exists", data.get("name"));
					//Note:  eliminated data.get("name") in error message as requested by Alstom (Andrey K.)
					throw new IntensWsException("A project with this name already exists.",  
							HttpStatus.BAD_REQUEST);
				}
			}

			logger.debug("Saving project by User '{}'", getUser());

			// set owner and group if not available
			if (!data.containsKey("owner") ) {
				logger.debug("set owner of a new project");
				Owner owner = getOwner();
				data.put("owner", owner.getUsername()); // ignore provided owner
				if (owner.getActiveGroup() == null) {
					throw new IntensWsException("No active group",
							HttpStatus.BAD_REQUEST);
				}
				data.put("group", owner.getActiveGroup().getName()); // ignore provided group
			}

			Project p = convertToProject(data, status);
			if (p == null)
				return Collections.emptyMap();

			// check if project new
			if (status.containsKey("status")
					&& status.get("status").equals("new")) {
				p.setId(null);
				p.setCreated(new Date());
				p.setStatus(p.getDefaultStatus("project"));
			}

			checkVersion(p, status, PROJECT_TYPE);
			if (status.containsKey("status")
					&& status.get("status").equals("upToDateConflict")) {
				throw new IntensWsException(status, HttpStatus.CONFLICT);
			}

			Map<String, Object> m = elementService.save(p.toMap(),
					PROJECT_TYPE, getUser(), reason);

			return m;

		} catch (CoreException e) {
			throw new IntensWsException(e, HttpStatus.BAD_REQUEST);

		} catch (UsernameNotFoundException e) {
			throw new IntensWsException("Owner not found: " + getUser(),
					HttpStatus.BAD_REQUEST);
		}

	}

	private List<Map<String, Object>> findByName(String name) {
		Map<String, Object> search = new HashMap<String, Object>();
		search.put("name", new SearchEq<String>(name));
		final String owner = null; // find for all owners
		final List<String> pnames = Arrays.asList("id",
				"created", "owner", "group");
		final Map<String, Map<String, Object>> childargs = null;
		int page = 0;
		int pagesize = 0;
		final Map<String, Integer> sortmap = null;
		logger.debug("search name {}", name);
		boolean latestRevision = true;
		return elementService.findByType(PROJECT_TYPE, owner, pnames, search,
				childargs, page, pagesize, sortmap, latestRevision);
	}

	public boolean isValidTransition( Project.Status from, Project.Status to){
		var validTo = env.getProperty("projectStatusTransitions." + from);
		return validTo != null && validTo.contains(to.toString());
	}

	/**
   * Check status of project
   *
	 * @param data The project to check status of
	 * @return
	 */
	@Secured("ROLE_USER")
	@PutMapping(path = "/projects/check")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExCheckStatus.class))
      })
  })
	public HashMap<String, Object> checkStatus(@Parameter(schema = @Schema(implementation =  ExProject.class)) @RequestBody Map<String, Object> data) {
		HashMap<String, Object> status = new HashMap<String, Object>();
		try {
			Project project = convertToProject(data, status);
			if (status.containsKey("status") || project == null) {
				return status;
			}
			// check for modifications
			Map<String, Object> modifications = elementService
					.getModifiedProperties(project.toMap());
			status.put("modifications", modifications);
			status.put("id", project.getId());
			if (modifications.isEmpty()) {
				logger.debug("Project not modified");
				status.put("status", "notModified");
                                status.put("version", findById(project.getId()).get("version"));
				return status;
			}
			if( logger.isDebugEnabled() ) {
				for( String key : modifications.keySet() ) {
					logger.debug("Key {}: {}", key, modifications.get(key));
				}
			}
			logger.debug("Project is modified");
			status.put("status", "modified");
			status.put("username", getOwner().getFullName());
			status.put("group", project.getGroup().getName());

			// check state transition
			Map<String, Object> m = findById(project.getId());
			String stat = (String) m.get("status");
			if (stat != null) {
				Project.Status from = Project.Status.get(stat);
				logger.debug("Project: check status transition from {} to {}",
						from, project.getStatus());
				if (!isValidTransition(from, project.getStatus())) {
					status.put("status", "error");
					status.put("text", "invalid status transition: " + from
							+ " --> " + project.getStatus());
					return status;
				}
			}
			logger.debug("Project: status transition check: passed");

			applyRules(project);
			if (!project.isValid()) {
				status.put("status", "error");
				status.put("text", project.getError() );
				return status;
			}

			checkVersion(project, status, PROJECT_TYPE);
			return status;

			// } catch (CoreException e) {
			// throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
		} catch (UsernameNotFoundException e) {
			throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
		} catch (IllegalArgumentException e) {
			status.put("status", "error");
			status.put("text", e.getMessage());
			// throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
		}
		return status;
	}

	// return project if modified
	private Project convertToProject(Map<String, Object> projMap,
			HashMap<String, Object> status) {
		// add id, type, name and revision to status
		status.put("type", PROJECT_TYPE);
		if (projMap != null) {
			if (projMap.containsKey("name"))
				status.put("name", projMap.get("name"));
			if (projMap.containsKey("id"))
				status.put("id", projMap.get("id"));
		}

		Set<String> keylist = projMap == null ? new HashSet<String>()
				: new HashSet<String>(projMap.keySet());
//		for (String key : Arrays.asList(new String[] { "type", "id", "group",
//				"owner", "version" })) {
		for (String key : Arrays.asList("type", "id", "version")) {
			keylist.remove(key);
		}
		if (keylist.isEmpty()) { // we don't save empty projects
			status.put("status", "notModified");
			logger.debug("Empty project -> Noting modified");
			return null;
		}

		Project project = new Project(projMap, componentProperties);
		if (!projMap.containsKey("id")) { // New project
			logger.debug("New project");
			status.put("status", "new");
			return project;
		}

		List<Map<String, Object>> projlist = findByName(project.getName());
		if (logger.isDebugEnabled()) {
			for (Map<String, Object> m : projlist) {
				logger.debug("Project id {}", m.get("id"));
			}
		}
		if (projlist.isEmpty()) { // New project
			logger.debug("Project was not found -> New one");
			status.put("status", "new");
			return project;
		}
		project.setId(null); // assume new project and check if
		// a project with same id and name exists
		for (Map<String, Object> pm : projlist) {
			Long id = ((Number) projMap.get("id")).longValue();
			if (pm.get("id").equals(id)) { // existing project
				project.setId(id);
				project.setCreated((Date) pm.get("created"));
				project.setOwner((String) pm.get("owner"));
				project.setGroup((String) pm.get("group"));
				break;
			}
		}
		if (project.getId() == null) {
			status.put("status", "new");
		}
		return project;
	}

	private void applyRules(Project project) throws UsernameNotFoundException {
		User currentUser = new User(getOwner());
		logger.debug("current user " + currentUser);

		project.setNextGroup(project.getGroup().getName());
		project.setNextOwner(project.getOwner().getUsername());

		Map<String, Object> p = findById(project.getId());

		String fromGroup = (String) p.get("group");
		project.setGroup(fromGroup);

		//Owner
		String fromOwner = (String) p.get("owner");
		project.setOwner(fromOwner);

		List<Command> cmds = new ArrayList<>();

		cmds.add(CommandFactory.newInsert(project, "project"));
		cmds.add(CommandFactory.newInsert(project.getGroup(), "group"));
		cmds.add(CommandFactory.newInsert(currentUser, "currentUser"));
		ExecutionResults results = kieSession.execute(CommandFactory
				.newBatchExecution(cmds));


		if( fromGroup.equals(project.getGroup().getName()) ) {
			project.setGroup(project.getNextGroup().getName());
		}

		if( fromOwner.equals(project.getOwner().getUsername()) ) {
			project.setOwner(project.getNextOwner().getUsername());
		}

	}

	/**
   * Rename a project
   *
	 * @param id Id of project to rename
	 * @param newname New name of project
	 * @return The renamed project
	 */
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	@Transactional
	@PutMapping(path="/projects/{id}/rename")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExProject.class))
      })
  })
	public Map<String, Object> rename(@PathVariable("id") Long id, @RequestBody String newname, @RequestParam(required = false, defaultValue = "rename") String reason) {
		Map<String, Object> p = findById(id);
		Project project = new Project(p, componentProperties);
		// strip leading and trailing quotation chars
		newname = newname.substring(1, newname.length() - 2);
		List<Map<String, Object>> plist = findByName(newname);
		logger.debug("newname {} plist size {}", newname, plist.size());
		if (!plist.isEmpty()) {
			for (Map<String, Object> pmap : plist) {
				logger.debug(pmap.toString());
			}
			throw new IntensWsException("Project " + newname
					+ " already exists.", HttpStatus.BAD_REQUEST);
		}
		project.setName(newname);
		// check permission
		try {
			applyRules(project);
			if (!project.isValid()) {
				throw new IntensWsException(project.getError(),
						HttpStatus.BAD_REQUEST);
			}
			logger.debug("Project: permission check for user {} passed",
					getUser());

			logger.debug("Project {}", project.toMap().toString());
			return elementService.save(project.toMap(), PROJECT_TYPE,
					getUser(), reason);

		} catch (CoreException e) {
			throw new IntensWsException(e, HttpStatus.BAD_REQUEST);

		} catch (UsernameNotFoundException e) {
			throw new IntensWsException("Owner not found: " + getUser(),
					HttpStatus.BAD_REQUEST);
		}
	}

	/**
   * Delete a project
	 * @param id Id of project to delete
	 */
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	@Transactional
	@DeleteMapping(path = "/projects/{id}")
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
	public void delete(@PathVariable("id") Long id) {
		logger.debug("Deleting project with id: {}", id);
		Map<String, Object> p = findById(id);
		if (id == null) {
			throw new IntensWsException("Project " + id + " not found.",
					HttpStatus.BAD_REQUEST);
		}
		Project project = new Project(p, componentProperties);
		// check owner
		Owner owner = getOwner();
		if (!owner.getUsername().equals(project.getOwner().getUsername())) {
			throw new IntensWsException("Owner conflict " + project.getName(),
					HttpStatus.BAD_REQUEST);
		}
		logger.debug("Project {}", project.toMap().toString());
		logger.debug("Deleting variants");
		deleteVariants( project.getId(), owner);
		try {
			logger.debug("Try to delete project");
			elementService.deleteElement(id);
			logger.debug("Project deleted");
		} catch (CoreException e) {
			throw new IntensWsException("Owner conflict " + project.getName(),
					HttpStatus.BAD_REQUEST);
		}

	}
	  private void deleteVariants(Long projectId, Owner owner ) {
			String anyOwner = null; // any owner
			final Map<String, Map<String, Object>> nochildargs = null;
			int page=0;
			int pagesize=0;
			Map<String, Integer> sortmap = null;
			Map<String, Object> search = new HashMap<String, Object>();
			boolean latestRevision = true;
			List<Long> variantids = new ArrayList<Long>();
		    if( projectId==null ){
		      throw new IntensWsException("Project Id is null", HttpStatus.BAD_REQUEST);
		    }
		    // TODO check if search works
			search.put("projectId", new SearchEq<Long>(projectId));
			logger.debug("find {} {}", VARIANT_TYPE, search);
			for( Map<String, Object> v: elementService.findByType( VARIANT_TYPE, anyOwner,
						Arrays.asList("name", "created", "desc", "owner", "group", "approval"),
						search, nochildargs,page,pagesize,sortmap, latestRevision) ) {

				Variant variant = baseEntityFactory.createVariant(v, null);
				// check owner
				if( !owner.getUsername().equals(variant.getOwner().getUsername() )){
					throw new IllegalArgumentException("Owner of variant "+variant.getName()+ " is not you.");
				}
				// check approval state
				if( variant.getApprovalState()!=ApprovalState.InPreparation &&
						variant.getApprovalState()!=ApprovalState.Experimental){
					throw new IntensWsException("Cannot delete variant " + variant.getName() +
							" approval state "+variant.getApprovalState(),
							HttpStatus.BAD_REQUEST);
				}
				variantids.add( variant.getId() );
			}
		    try {
		    	for( Long id: variantids ){
		    		elementService.deleteElement(id);
		    	}

		    } catch (CoreException e) {
		      throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
		    }
		  }

  /**
   * import a component (keeps creation date)
   * @param compdata component data
   * @return import component
   **/
  @Secured("ROLE_ADMIN")
  @PutMapping(path = "/projects/import")
  @Transactional
  @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
  )
  public Map<String, Object> create(@RequestBody Map<String, Object> compdata) {
    Project proj = null;
    String ownername = null;
    try {
      // missing owner -> return BAD_REQUEST
      if (!compdata.containsKey("owner") || ((String) compdata.get("owner")).isEmpty()) {
        logger.warn("No owner found");
        throw new IntensWsException("No owner found", HttpStatus.BAD_REQUEST);
      }
      ownername = (String) compdata.get("owner");
      Owner owner = userService.findOwnerByUsername(ownername);
      proj = convertToProject(compdata, new HashMap<>());

      proj.setId(null);
      String reason = compdata.containsKey("reason") ? (String) compdata.get("reason") : "imported";
      return elementService.save(proj.toMap(), PROJECT_TYPE, owner.getUsername(), reason);

    } catch (IllegalArgumentException e) {
      throw new IntensWsException(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (CoreException e) { // up-to-date conflict
      throw new IntensWsException(
          "<b>" + e.getMessage() + "</b><br/>" + " " + proj + "<br/>",
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
   * Update a project
   *
	 * @param data New project data
	 * @param id Id of the project
	 * @return The updated project
	 */
	@Secured({"ROLE_USER", "ROLE_ADMIN"})
	@PutMapping(path = "/projects/{id}")
	@Transactional
    @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    },
    responses = {
      @ApiResponse(content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ExProject.class))
      })
  })
	public Map<String, Object> update(@Parameter(schema = @Schema(implementation = ExProject.class)) @RequestBody HashMap<String, Object> data, @PathVariable("id") Long id) {
		boolean nameChanged = false;
		Map<String, Object> project_as_map = findById(id);

		if( data.isEmpty() ) return project_as_map;

		logger.debug("Update project");
		// Load the variant to get to projectID
		logger.debug("Step 1 of 6: Load project by id {}", id);
		if (project_as_map == null) {
			logger.error("FAIL step 1 of 8: No project found with id: {}", id);
			throw new IntensWsException("Project with id: " + id + " not found", HttpStatus.BAD_REQUEST);
		}

		logger.debug("Step 2 of 6: Check if name is in data and if name exists, check if names is allowed");
		// If name is present and the name is changed -> Check if new name is
		// allowed
		if (data.containsKey("name")) {
			String name = (String) data.get("name");
			if (!name.equals(project_as_map.get("name"))) {
				nameChanged = true;
				// Load all projects with this name
				List<Map<String, Object>> prolist = findByName((String) data.get("name"));
				if (!prolist.isEmpty()) {
					logger.error("FAIL: 2 of 7: Name {} already exists", data.get("name"));
					//Note:  eliminated data.get("name") in error message as requested by Alstom (Andrey K.)
					throw new IntensWsException("A project with this name already exists.",  
							HttpStatus.BAD_REQUEST);
				}
			}
		}

		logger.debug("Step 3 of 6: apply new data");
		// updated_project is now fully loaded from database with the new
		// attributes:
		Map<String, Object> updated_project = updateElementValues(data, id);

		HashMap<String, Object> status = new HashMap<String, Object>();

		logger.debug("Step 4 of 6: Check reason");
		String reason = "";
		if (updated_project.containsKey("interactiveReason")) {
			reason = (String) updated_project.get("interactiveReason");
			updated_project.remove("interactiveReason");

		// reason missing or provided by application -> check status
		} else {
			// reason provided by application -> use it
			boolean reasonMissing = true;
			if (updated_project.containsKey("reason")) {
				reason = (String) updated_project.get("reason");
				updated_project.remove("reason");
				reasonMissing = false;
			}

			logger.debug("No interactive reason found -> check");
			status = checkStatus(updated_project);

			if (status.get("status").equals("error")) {
				logger.error("Status error in check: {}", status.get("text"));
				throw new IntensWsException((String) status.get("text"), HttpStatus.BAD_REQUEST);
			}

			if ( nameChanged ) {
				// If the name changed we have to add the modification to the the modifications
				status.put("status", "modified");
				// If some modifications already exists
				if( status.containsKey("modifications")) {
					Map<String, Object> modifications = (Map<String, Object>) status.get("modifications");
					modifications.put("name", Arrays.asList(project_as_map.get("name"), data.get("name")));
				} else {
					// If no modification exists
					Map<String, Object> modifications = new HashMap<String, Object>();
					modifications.put("name", Arrays.asList(project_as_map.get("name"), data.get("name")));
					status.put("modification", modifications);
				}
			}

			if (status.get("status").equals("notModified") ) {
				logger.debug("Project is not modified");
				return project_as_map;
			}
			if (status.get("status").equals("error")) {
				logger.error("Status error in check: {}", status.get("text"));
				throw new IntensWsException((String) status.get("text"), HttpStatus.BAD_REQUEST);
			}
			if( status.get("status").equals("new")) {
				logger.error("New project in update does not work");
				throw new IntensWsException("Project was new in update", HttpStatus.BAD_REQUEST);
			}
			// up-to-date conflict or missing reason -> return status (conflict)
			if (status.get("status").equals("upToDateConflict") || reasonMissing) {
				logger.info("Up to date conflict or reason missing");
				throw new IntensWsException(status, HttpStatus.CONFLICT);
			}
		}
		logger.debug("Reason found: '{}' continue save", reason);

		if (nameChanged) updated_project.put("name", data.get("name"));
		Project p = convertToProject(updated_project, status);
		try {
			logger.debug("Step 5 of 6: Check version");
			checkVersion(p, status, PROJECT_TYPE);
			if (status.containsKey("status") && status.get("status").equals("upToDateConflict")) {
				throw new IntensWsException(status, HttpStatus.CONFLICT);
			}
			logger.debug("Step 6 of 6: Store project");
			return elementService.save(updated_project, PROJECT_TYPE, getUser(), reason);

		} catch (UsernameNotFoundException e) {
			logger.error("FAIL step 6 of 6: Save not possible, the  username {} was not found", getUser());
			throw new IntensWsException(e, HttpStatus.BAD_REQUEST);

		} catch (CoreException e) {
			logger.error("FAIL step 6 of 6: Save not possible, core exception: {}", e);
			throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
		}
	}
}
