package ch.semafor.intens.ws.service;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Modification;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.ApprovalState;
import ch.semafor.intens.ws.model.Component;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.model.Variant;
import ch.semafor.intens.ws.model.swagger.ExQueryParams;
import ch.semafor.intens.ws.model.swagger.ExVariant;
import ch.semafor.intens.ws.utils.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/services/rest")
@Transactional
public class VariantsService extends BaseServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(VariantsService.class);

    @Autowired
    ComponentProperties componentProperties;

    @Autowired
    BaseEntityFactory baseEntityFactory;

    @Autowired
    ApprovalStateTransition approvalStateTransition;

    /**
     * Find single variant by id
     *
     * @param id Id of the variant
     * @return The variant
     */
    @GetMapping(path = "/variants/{id}")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")},
            responses = {
                    @ApiResponse(content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ExVariant.class))
                    })
            })
    public Map<String, Object> findById(@PathVariable("id") Long id) {
        logger.debug("find variant by id {}", id);
        return elementService.getElementMap(id);
    }

    /**
     * Get all variants with an optional query
     *
     * @param queryParams The query to search with
     * @return Matched variants
     */
    @GetMapping(path = "/variants")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")},
            responses = {
                    @ApiResponse(content = {
                            @Content(mediaType = "application/json",
                                     array = @ArraySchema(schema = @Schema(implementation = ExVariant.class)))
                    })
            })
    public List<Map<String, Object>> find(@Parameter(schema = @Schema(implementation = ExQueryParams.class))
                                          @RequestParam Map<String, String> queryParams) {
        /*   maxChange, _projection,owner,id,page,pagesize,ignorecase,sort */

        List<String> minimal = new ArrayList<>(Arrays.asList(
                "name", "created", "projectId", "owner", "ownername", "group", "approval")
        ); // mandatory fields
        int maxChangePeriod = -1;
        int kindChange = Calendar.MONTH;  // default
        String maxChange = queryParams.get("maxChange");
        if (maxChange != null) {
            minimal.add("changed");

            if (maxChange.endsWith("D")) {
                kindChange = Calendar.DAY_OF_YEAR;
                maxChange = maxChange.replaceAll(".$", "");
            } else if (maxChange.endsWith("M")) {
                kindChange = Calendar.MONTH;
                maxChange = maxChange.replaceAll(".$", "");
            } else if (maxChange.endsWith("Y")) {
                kindChange = Calendar.YEAR;
                maxChange = maxChange.replaceAll(".$", "");
            }
            try {
                maxChangePeriod = Integer.parseInt(maxChange);
            } catch (NumberFormatException ex) {
                logger.warn("Parameter '{}' of maxChange not valid", maxChange);
            }
            queryParams.remove("maxChange");
        }
        List<String> fields = extractFields(queryParams, minimal);
        String owner = extractOwnerName(queryParams);
        Map<String, Integer> sortmap = extractSort(queryParams);
        int page = extractPage(queryParams);
        int pagesize = extractPageSize(queryParams);
        Map<String, Object> search = extractSearchArgs(queryParams);

        Date changedSince = null;

        if (maxChangePeriod >= 0) {
            Calendar cal = new GregorianCalendar();
            cal.add(kindChange, -maxChangePeriod);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            changedSince = cal.getTime();
        }

        List<Map<String, Object>> l = new ArrayList<>();
        String id = queryParams.get("id");
        if (id != null) {
            logger.debug("find project by id(s) {}", id);
            try {
                Map<String, Object> v = elementService.getElement(Long.valueOf(id), fields);
                if (v != null && !v.isEmpty()) {
                    l.add(v);
                }
            } catch (NumberFormatException ex) {
                throw new IntensWsException("Invalid id '" + id + "'" + " for variant",
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            Map<String, Map<String, Object>> childsearch = new HashMap<String, Map<String, Object>>();
            if (!buildSearch(VARIANT_TYPE, queryParams, search, childsearch)) {
                logger.info("buildsearch disabled");
                return l;
            }
            logger.debug("find {} {}", VARIANT_TYPE, search);
            boolean latestRevision = true;
            l = elementService.findByType(VARIANT_TYPE, owner, fields,
                    search, childsearch, page, pagesize, sortmap, latestRevision, changedSince);
        }

        AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);
        logger.debug("found {} variants", l.size());

        if (!this.hasAdminRole()) {
            l = accessFilter.filterVariants(l);
            logger.debug("--> {} remaining variants after filterVariants()", l.size());
        }

        return l;
    }

    /**
     * Get all variants of a project
     *
     * @param projectId Id of the project
     * @return Array of variants matched to the project
     */
    @GetMapping(path = "/variants/project/{id}")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")},
            responses = {
                    @ApiResponse(content = {
                            @Content(mediaType = "application/json",
                                     array = @ArraySchema(schema = @Schema(implementation = ExVariant.class)))
                    })
            })
    public List<Map<String, Object>> findByProject(@PathVariable("id") Long projectId) {
        final String anyOwner = null; // any owner
        final Map<String, Map<String, Object>> nochildargs = null;
        int page = 0;
        int pagesize = 0;
        Map<String, Integer> sortmap = null;
        Map<String, Object> search = new HashMap<>();
        search.put("projectId", new SearchEq<>(projectId));
        AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);

        logger.debug("find {} {}", VARIANT_TYPE, search);
        boolean latestRevision = true;
        List<Map<String, Object>> variants = elementService.findByType(VARIANT_TYPE, anyOwner,
                Arrays.asList("name", "created", "desc", "owner", "ownername", "group", "approval"),
                search, nochildargs, page, pagesize, sortmap, latestRevision);

        if (!this.hasAdminRole()) {
            return accessFilter.filterVariants(variants);
        }
        return variants;
    }

    /**
     * return list of variants that have component
     *
     * @param compId  Id of the component
     * @param varcomp
     * @return Variants matched to the component
     */
    @GetMapping(path = "/variants/component/{id}")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")},
            responses = {
                    @ApiResponse(content = {
                            @Content(mediaType = "application/json",
                                     array = @ArraySchema(schema = @Schema(implementation = ExVariant.class)))
                    })
            })
    public List<Map<String, Object>> findByComponent(@PathVariable("id") Long compId,
                                                     @RequestParam("varcomp") String varcomp) {
        final String anyOwner = null;
        final Long anyProjectId = null;
        return findVariants(anyOwner, anyProjectId, compId, varcomp);
    }

    /**
     * return list of variants of this project that have component
     *
     * @param projId  Id of the project
     * @param compId  Id of the component
     * @param varcomp
     * @return All matched variants
     */
    @GetMapping(path = "/variants/project/{pid}/component/{cid}")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")},
            responses = {
                    @ApiResponse(content = {
                            @Content(mediaType = "application/json",
                                     array = @ArraySchema(schema = @Schema(implementation = ExVariant.class)))
                    })
            })
    public List<Map<String, Object>> findByProjectAndComponent(@PathVariable("pid") Long projId,
                                                               @PathVariable("cid") Long compId,
                                                               @RequestParam String varcomp) {
        logger.debug("Search variants with projId = {} compId = {}", projId, compId);

        final String anyOwner = null;
        return findVariants(anyOwner, projId, compId, varcomp);
    }

    private List<Map<String, Object>> findVariants(String owner, Long projId, Long compId,
                                                   String varcomp) {
        Map<String, Object> search = null;
        if (projId != null) {
            search = new HashMap<>();
            search.put("projectId", new SearchEq<>(projId));
        }
        Map<String, Object> m = new HashMap<>();
        m.put("compId", new SearchEq<>(compId.intValue()));

        List<Map<String, Object>> result = new ArrayList<>();

        AccessFilter accessFilter = new AccessFilter(getOwner(), componentProperties);
        for (String refname : getVarCompNames(varcomp)) {
            Map<String, Map<String, Object>> varcompsearch = new HashMap<>();
            varcompsearch.put(refname, m);
            logger.debug("Search variants ref = {}", refname);
            boolean latestRevision = true;
            result.addAll(elementService.findByType(
                    VariantsService.VARIANT_TYPE, owner,
                    Arrays.asList("name", "created", "rev", "desc", "owner", "ownername", "group", "approval",
                            "projectId"),
                    search, varcompsearch, 0, 0, null, latestRevision));
            if (search != null) {
                logger.debug("search {}", search);
            }
            logger.debug("varcompsearch {}", varcompsearch);
            logger.debug("variants size {}", result.size());
        }
        return accessFilter.filterVariants(result);
    }

    private void checkIfNewOrUpdate(Variant variant) {
    	String owner = null;
		if (componentProperties.useOwnerNameRevisionAsIdentifier){
			owner=variant.getOwner().getUsername();
		}
		List<Map<String, Object>> varlist = findByNameAndRevAndProjId(owner,
                variant.getName(), variant.getRevision(), variant.getProjId());
        if (!varlist.isEmpty()) {
            // Variants were found -> Use this: Update the one with the same revision
            Map<String, Object> var_db = varlist.get(0);
            logger.debug("Found a variant with same name and revision and project_id: Update variant");
            variant.setId((Long) var_db.get("id"));
            variant.setCreated((Date) var_db.get("created"));
            variant.getDbStatus().put("status", "update");

            // This method only was called when save so set the owner and name from DB
            variant.setOwner((String) var_db.get("owner"));
            variant.setGroup((String) var_db.get("group"));
            logger.debug("Set owner: {}", variant.getOwner().getUsername());
            logger.debug("Set group: {}", variant.getGroup().getName());

            // run transition check because we have the variant from db
            String app = (String) var_db.get("approval");

            if (app != null) {
                ApprovalState from = ApprovalState.get(app);
                logger.debug("Variant: check approval state transition from {} to {}",
                        from, variant.getApprovalState());
                if (!approvalStateTransition.isValid(from, variant.getApprovalState())) {
                    variant.getDbStatus().put("status", "error");
                    variant.getDbStatus().put("text", "Variant invalid state transition: " +
                            from + " --> " + variant.getApprovalState());
                    logger.warn("Variant: approval state transition check: FAILED");
                    return;
                }
            }
            logger.debug("Variant: approval state transition check: passed");
            return;
        }
        // Variant was not found

        // check if creation is allowed
        final Long anyId = null;
        if(getMaxRevision(VARIANT_TYPE, variant.getName(), variant.getProjId(), owner, anyId) >= 0) {
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
            logger.warn("A variant with same name but different revision exists");
            variant.getDbStatus().put("status", "error");
            variant.getDbStatus().put("text",
                    VARIANT_TYPE + " " + variant.getName() +
                    " exists already. Please choose a different name.");
            return;
        }

		// Set owner and group
        logger.debug("Variant {} (Rev. {}) was not found in project {} -> New Variant",
                variant.getName(), variant.getRevision(), variant.getProjId());
        variant.getDbStatus().put("status", "new");
        Owner o = getOwner();
        variant.setOwner(o.getUsername());
        variant.setGroup(o.getActiveGroup().getName());
        variant.getDbStatus().remove("rev");
        variant.setId(null); // Delete ID to avoid update
        // TODO: ApprovalState.Default
        boolean rev0 = true;
        variant.setApprovalState(componentProperties.getDefaultApprovalState(
        		"component", rev0));
    }

    private void setModifications(Variant variant) {
        if (variant.getDbStatus().get("status").equals("error") ||
                variant.getDbStatus().get("status").equals("new")) {
            return;
        }
        Map<String, Object> modifications = elementService.getModifiedProperties(variant.toMap());
        if (modifications.isEmpty()) {
            logger.debug("Variant not modified");
            variant.getDbStatus().put("status", "notModified");
            variant.getDbStatus().put("version", findById(variant.getId()).get("version"));
            return;
        }
        variant.getDbStatus().put("modifications", modifications);
        if (logger.isDebugEnabled()) {
            logger.debug("Variant is modified:");
            for (String key : modifications.keySet()) {
                logger.debug("  {} -> {}", key, modifications.get(key).toString());
            }
        }
        variant.setModified(modifications);
    }

    /**
     * Save a variant
     *
     * @param data Data of the variant
     * @return The saved variant
     */
    @PutMapping(path = "/variants")
    @Transactional
    @Operation(security = {
            @SecurityRequirement(name = "bearer-key")
    }
    )
    public Map<String, Object> save(@RequestBody Map<String, Object> data) {
        logger.debug("Start: Save variant");
        if (data == null || data.get("projectId") == null) {
            logger.warn("throw IntensWsEcxeption: no project id");
            throw new IntensWsException("missing project id", HttpStatus.BAD_REQUEST);
        }
        Variant variant = null;

        // Status: unknown

        // default owner just in case
        Owner owner = getOwner();

        variant = baseEntityFactory.createVariant(data, owner);
        // return if creation of variant gives an error
        if (variant.getDbStatus().get("status").equals("error")) {
            logger.warn("throw IntensWsEcxeption: error in status");
            throw new IntensWsException((String) variant.getDbStatus().get("text"),
                    HttpStatus.BAD_REQUEST);
        }
        // return if not modified
        if (variant.getDbStatus().get("status").equals(
                "notModified")) { // This can only be notModified if only id, rev and version was send
            logger.debug("Variant not modified -> return");
            return Collections.emptyMap();
        }

        // We have a valid variant -> Check if new or update
        checkIfNewOrUpdate(variant);

        // Status: new / update / error
        String reason = variant.checkReason((HashMap<String, Object>) data);

        if (variant.getDbStatus().get("status").equals("update")) {
            logger.debug("Set modifications for update a variant");
            setModifications(variant);
        }

        // No modifications
        if (variant.getDbStatus().get("status").equals("notModified")) {
            if (variant.getId() != null) {
                logger.info("Load variant from id");
                logger.debug("Variant not modified -> return");
                return findById(variant.getId());
            }
            logger.debug("return empty Map <- new Component");
            return Collections.emptyMap();
        }

        if (variant.getDbStatus().get("status").equals("error")) {
            logger.warn("throw IntensWsEcxeption: error in status");
            throw new IntensWsException((String) variant.getDbStatus().get("text"),
                    HttpStatus.BAD_REQUEST);
        }
        // Status: New or modified

        // Apply rules;
        int maxRevision = -1;
        if(!variant.isNew()) {
            final String anyOwner = null;
        	final Long anyId = null;
        	maxRevision = getMaxRevision(VARIANT_TYPE, variant.getName(),
        			variant.getProjId(), anyOwner, anyId);
        	variant.applyRules(false,
        			elementService, new User(owner), kieSession, maxRevision);
        }

        // Up to date conflict
        try {
            checkVersion(variant, variant.getDbStatus(), VARIANT_TYPE);
        } catch (UsernameNotFoundException e) {
            throw new IntensWsException((String) variant.getDbStatus().get("text"),
                    HttpStatus.BAD_REQUEST);
        }

        if (variant.getDbStatus().get("status").equals("error")) {
            logger.warn("Check component failed -> status: {} -> throw IntensWsException",
                    variant.getDbStatus());
            throw new IntensWsException((String) variant.getDbStatus().get("text"),
                    HttpStatus.BAD_REQUEST);
        }

        // No reason
        if (reason == null) {
            logger.warn("No interactive reason or reason found status: {} -> throw IntensWsException",
                    variant.getDbStatus());
            throw new IntensWsException(variant.getDbStatus(), HttpStatus.CONFLICT);
        }

        // Up to date conflict
        if (variant.isUpToDateConflict()) {
            logger.warn("Up-to-date conflict status: {} -> throw IntensWsException",
            		variant.getDbStatus());
            throw new IntensWsException(variant.getDbStatus(), HttpStatus.CONFLICT);
        }

        if (variant.isNew()) {
            logger.debug("New Variant");
            variant.setCreated(new Date());
            variant.setNextRevision(maxRevision);
            variant.incRevision();
            // TODO: ApprovalState.Default
            variant.setApprovalState(componentProperties.getDefaultApprovalState(
            		"component", variant.getRevision() == 0));
        }

        checkApprovalStateOfComponents(variant);
        if (variant.getDbStatus().get("status") != null && variant.getDbStatus().get("status")
                .equals("error")) {
            logger.warn("Error in check ApprovalStateOfComponent {}", variant.getDbStatus().get("text"));
            throw new IntensWsException("Could not store variant cause\n"
                    + variant.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
        }

        logger.debug("All components are in correct approval");

        try {
            Map<String, Object> saved_variant = elementService
                    .save(variant.toMap(), VARIANT_TYPE, getUser(), reason);
            logger.debug("after save: {}", saved_variant.toString());
            logger.debug("Variant saved -> return");
            return saved_variant;

        } catch (CoreException e) {
            logger.warn("throw IntensWsEcxeption: CoreException");
            throw new IntensWsException(e, HttpStatus.BAD_REQUEST);

        } catch (UsernameNotFoundException e) {
            throw new IntensWsException("Owner not found: " + getUser(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void checkApprovalStateOfComponents(Variant variant) {
        /**
         * Check if the components which are attached to the variant have the
         * same or higher approval state
         *
         * 1. Get all components which are attached to this variant
         * 2. Check if the approval state is OK (valid)
         * 3. If not change the approval state and save the component
         *
         */

        // To check which approval state should be checked for snapshot
        // Ignore these approval states -> We are only interested in these 3:
        // Approved
        // Tested
        // Tendered
        if (ApprovalStateConsistency.isSnapshot(componentProperties, variant.getApprovalState())) {
            return;
        }
        logger.debug("Checking approval state for attached components");

        // 1. Get all components
        List<Map.Entry<BigInteger, String>> components = variant.getComponents();

        // No components -> Nothing to do!
        if (components == null || components.isEmpty()) {
            logger.info("No components found for this variant");
            return;
        }

        // 2. Go through all components and check approval state
        for (Map.Entry<BigInteger, String> mkt: components) {
            // The Map always have only one entry {compId: type}
            // So we have to get the first key which is our compId
            BigInteger compId = mkt.getKey();
            logger.debug("CompID {}", compId);

            // Load component from database
            Map<String, Object> comp = findById(compId.longValue());
            // No component found -> Nothing to do
            if (comp == null || comp.isEmpty()) {
                logger.warn("component id {} not found", compId);
                // Component not found -> Should never happen
                continue;
            }
            // Important! add type! If we want to update a component, we have to set a type to avoid IntensWsException
            logger.debug("Add type to component {}", mkt.getValue());
            if(mkt.getValue() == null){
                logger.warn("Invalid type for Compid {}", compId);
                variant.getDbStatus().put("status", "error");
                variant.getDbStatus().put("text", "invalid type for compid " + compId);
                return;
            }
            comp.put("type", mkt.getValue());
            logger.debug("Check approval {} -> {}", variant.getApprovalState(), comp.get("approval"));

            // 3. Check if approval state is same or higher

            if (!ApprovalStateConsistency.isValid(this.componentProperties,
                    variant.getApprovalState(), ApprovalState.get((String) comp.get("approval"))
            )) {
                // Set modified
                variant.getDbStatus().put("status", "modified");
                try {
                    setApprovalStateForComponent(comp, variant.getApprovalState());
                } catch (IntensWsException e) {
                    variant.getDbStatus().put("status", "error");
                    variant.getDbStatus()
                            .put("text", variant.getDbStatus().get("text") + "\n" + e.getMessage());
                }
            }
        }
    }

    private void setApprovalStateForComponent(Map<String, Object> component,
                                              ApprovalState approvalState) {
        /**
         * Update the approval state of the component and store the component in the database
         * 1. Create object Component
         * 2. Set the correct attributes
         * 3. Check if component is valid -> applyRules
         * 4. Check up-to-date conflict
         * 5. Save the component with new approval state
         */

        // 1. Create Component
        logger.debug("Create component {}", component.toString());
        Component comp = baseEntityFactory.createComponent(component, getOwner());

        // 2. Set correct attributes -> Approval state
        comp.setApprovalState(approvalState);
        comp.getDbStatus().put("status", "modified");
        comp.setInteractiveReason(true);

        // 3. Check if component is valid
        final Long noProjectId = null;
        final String anyOwner = null;
        final Long anyId = null;
        comp.applyRules(false, elementService, new User(getOwner()), kieSession,
                getMaxRevision(comp.getType(), comp.getName(), noProjectId, anyOwner, anyId), "modify");

        // 4. Check if up to date conflict exists
        try {
            checkVersion(comp, comp.getDbStatus(), comp.getType());
        } catch (UsernameNotFoundException e) {
            throw new IntensWsException(comp.getDbStatus(), HttpStatus.BAD_REQUEST);
        }

        // Check status of our component
        if (comp.getDbStatus().get("status").equals("error")) {
            logger.warn("Check component failed -> status: {}", comp.getDbStatus());
            throw new IntensWsException((String) comp.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
        }

        // Up to date conflict
        if (comp.isUpToDateConflict()) {
            logger.warn("Up-to-date conflict status: {}", comp.getDbStatus());
            throw new IntensWsException(comp.getDbStatus(), HttpStatus.CONFLICT);
        }

        // This should never happen but you never know...
        if ((comp.isNew())) {
            throw new IntensWsException("Component is new -> Can not happen in update approval",
                    HttpStatus.BAD_REQUEST);
        }

        // 5. Save the component with the new approval state
        try {
            elementService.save(comp.toMap(), comp.getType(), getUser(),
                    "Update approval state cause of variant to " + approvalState.getName());
        } catch (UsernameNotFoundException e) {
            throw new IntensWsException(
                    "User not found for updating component in variant cause of approval change.",
                    HttpStatus.BAD_REQUEST);
        } catch (CoreException e) {
            throw new IntensWsException("Could not save component.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get a list of all variants with that name that are visible to the current user
     *
     * @param name Name to search
     * @return variants that are visible to the current user
     */
    private List<Map<String, Object>> findByNameAndVisibility(String name) {
        Map<String, Object> search = new HashMap<>();
        search.put("name", new SearchEq<>(name));

        List<String> pnames = new ArrayList<>(Arrays.asList("id", "name",
                "approval", "owner", "group"));

        List<Map<String, Object>> varlist = elementService.findByType(
                VARIANT_TYPE, null, pnames, search,
                null, 0, 0, null, true);

        if (varlist.isEmpty()) {
            return varlist;
        }
        var filter = new AccessFilter(getOwner(), componentProperties);
        return filter.filterVariants(varlist);
    }

    private List<Map<String, Object>> findByNameAndRevAndProjId(String owner, String name, Integer rev,
                                                                Long projectId) {
        if (projectId == null) {
            return new ArrayList<>();
        }
        Map<String, Object> search = new HashMap<>();
        search.put("name", new SearchEq<>(name));
        search.put("projectId", new SearchEq<>(projectId));

        // NOTE: using more than 2 search args can be slow
        //if (rev != null) {
        //  search.put("rev", new SearchEq<Integer>(rev));
        //}
        List<String> pnames = new ArrayList<>(Arrays.asList("id",
                "created", "owner", "group"));
        if (rev != null) {
            pnames.add("rev");
        }
        boolean latestRevision = true;
        List<Map<String, Object>> varlist = elementService.findByType(
                VARIANT_TYPE, owner, pnames, search,
                null, 0, 0, null, latestRevision);
        if (rev == null)
            return varlist;
        if (logger.isDebugEnabled()) {
            logger.debug("Found variants:");
            for (Map<String, Object> m : varlist) {
                for (String key : m.keySet()) {
                    logger.debug("  {} {}", key, m.get(key));
                }
            }
        }
        return varlist.stream()
                .filter(m -> Objects.equals(m.get("rev"), rev)).collect(Collectors.toList());
    }

    /**
     * Check status of variant
     *
     * @param data Data to check
     * @return status code and map with modified properties (if any)
     */
    @PutMapping(path = "/variants/check")
    @Operation(security = {
            @SecurityRequirement(name = "bearer-key")
    }
    )
    public Map<String, Object> checkStatus(@RequestBody Map<String, Object> data) {
        logger.debug("Start: check status for variant");

        Variant variant = baseEntityFactory.createVariant(data, getOwner());
        // check if createComponent gives an error
        if (variant.getDbStatus().get("status").equals("error") ||
                variant.getDbStatus().get("status").equals("notModified")) {
            logger.warn("create variant failed -> status: {}", variant.getDbStatus());
            return (HashMap<String, Object>) variant.getDbStatus();
        }
        // status: unknown

        // TODO: use checkIfNewOrUpdate(variant); as in ComponentsService

        // TODO: Check if try catch is still needed
        try {
            String ownername = null;
            if (componentProperties.useOwnerNameRevisionAsIdentifier){
                ownername=variant.getOwner().getUsername();
            }
            List<Map<String, Object>> varlist = findByNameAndRevAndProjId(ownername,
                    variant.getName(), variant.getRevision(), variant.getProjId());

            if (logger.isDebugEnabled()) {
                for (Map<String, Object> m : varlist) {
                    logger.debug("Variant id {} rev {}", m.get("id"), m.get("rev"));
                }
            }

            if (!varlist.isEmpty()) { // variants found, filter revision
                Map<String, Object> var_db = varlist.get(0);
                variant.setId((Long) var_db.get("id"));
                variant.setCreated((Date) var_db.get("created"));

                if (!data.containsKey("owner")) {
                    variant.setOwner((String) var_db.get("owner"));
                }
                if (!data.containsKey("group")) {
                    variant.setGroup((String) var_db.get("group"));
                }

                // check for modifications
                Map<String, Object> modifications = elementService.getModifiedProperties(variant.toMap());
                variant.getDbStatus().put("modifications", modifications);
                variant.getDbStatus().put("id", variant.getId());

                if (modifications.isEmpty()) {
                    logger.debug("Variant not modified");
                    variant.getDbStatus().put("status", "notModified");
                    variant.getDbStatus().put("version", findById(variant.getId()).get("version"));
                    logger.debug("Not modfied -> return");
                    return (HashMap<String, Object>) variant.getDbStatus();
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Variant is modified:");

                    for (String key : modifications.keySet()) {
                        logger.debug("  {} -> {}", key, modifications.get(key).toString());
                    }
                }

                variant.setModified(modifications);

                // check state transition
                Map<String, Object> m = findById(variant.getId());
                String app = (String) m.get("approval");

                if (app != null) {
                    ApprovalState from = ApprovalState.get(app);
                    logger.debug("Variant: check approval state transition from {} to {}",
                            from, variant.getApprovalState());

                    if (!approvalStateTransition.isValid(from, variant.getApprovalState())) {
                        variant.getDbStatus().put("status", "error");
                        variant.getDbStatus().put("text", "Variant invalid state transition: " +
                                from + " --> " + variant.getApprovalState());
                        logger.debug("Error in variant -> return");
                        return (HashMap<String, Object>) variant.getDbStatus();
                    }
                }
                logger.debug("Variant: approval state transition check: passed");
                final Long anyId = null;
                final String anyOwner = null;
                final int maxRevision = getMaxRevision(VARIANT_TYPE, variant.getName(),
                                                       variant.getProjId(), anyOwner, anyId);
                Owner owner = getOwner();
                variant.applyRules(false, elementService, new User(owner), kieSession, maxRevision);
                if (variant.getId() == null) { // eventually a new variant?
                    variant.getDbStatus().put("status", "new");
                    variant.getDbStatus().remove("rev");
                    logger.debug("New variant -> return");
                    return (HashMap<String, Object>) variant.getDbStatus();
                }

                logger.debug("Variant: rules check: {}", variant.isValid());
                if (!variant.isValid()) {
                    variant.getDbStatus().put("status", "error");
                    variant.getDbStatus().put("text", "permission denied. Cannot update variant: "
                            + variant + "\nUser:" + getUser());
                    variant.getDbStatus().put("group", variant.getGroup().getName());
                    variant.getDbStatus().put("username", getOwner().getFullName());
                    logger.debug("Error in variant -> return");
                    return (HashMap<String, Object>) variant.getDbStatus();
                }

                if (!elementService.checkVersion(variant.getId(), variant.getVersion(), VARIANT_TYPE)) {
                    logger.warn("Version conflict for {} version {}", variant.getId(), variant.getVersion());
                    variant.getDbStatus().put("status", "upToDateConflict");
                    variant.getDbStatus().put("type", VARIANT_TYPE);
                    variant.getDbStatus().put("id", variant.getId());
                    variant.getDbStatus().put("name", variant.getName());
                    variant.getDbStatus().put("rev", variant.getRevision());
                    variant.getDbStatus().put("version", varlist.get(0).get("version"));
                    // date and user of last modification
                    String username = variant.getOwner().getUsername();
                    Date changed = variant.getCreated();
                    Modification mod = elementService.getLatestModification(variant.getId());
                    if (mod != null) {
                        username = mod.getUser().getUsername();
                        changed = mod.getTimestamp();
                    }

                    variant.getDbStatus().put("group", variant.getGroup().getName());
                    variant.getDbStatus().put("username", getOwner().getFullName());
                    Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    variant.getDbStatus().put("changed", formatter.format(changed));
                    variant.getDbStatus().put("changer", formatter.format(username));
                    logger.debug("UpTpDate conflict in variant -> return");
                    return (HashMap<String, Object>) variant.getDbStatus();
                }

                logger.debug("Variant: up-to-date conflict check: passed");
                variant.getDbStatus().put("status", variant.getDbStatus().get("status"));
                variant.getDbStatus().put("substatus", variant.getDbStatus().get("substatus"));
                variant.getDbStatus().put("group", variant.getGroup().getName());
                variant.getDbStatus().put("username", getOwner().getFullName());

                if (variant.getDbStatus().containsKey("approval")) {
                    variant.getDbStatus().put("approval", variant.getDbStatus().get("approval"));
                }

                logger.debug("variant OK -> return status {}", variant.getDbStatus().toString());
                return (HashMap<String, Object>) variant.getDbStatus();
            }

            // check if creation is allowed
            final Long anyId = null;
            if(getMaxRevision(VARIANT_TYPE, variant.getName(), variant.getProjId(), ownername, anyId) >= 0) {
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
                logger.warn("A variant with same name but different revision exists");
                variant.getDbStatus().put("status", "error");
                variant.getDbStatus().put("text",
                        VARIANT_TYPE + " " + variant.getName() +
                        " exists already. Please choose a different name.");
                return (HashMap<String, Object>) variant.getDbStatus();
            }

            logger.info("New variant");
            variant.getDbStatus().put("status", "new");
            variant.getDbStatus().remove("rev");
            logger.debug("New variant -> return");
            return (HashMap<String, Object>) variant.getDbStatus();

        } catch (IllegalArgumentException e) {
            variant.getDbStatus().put("status", "error");
            variant.getDbStatus().put("text", e.getMessage());
            logger.debug("IllegalArgumentException in checkStatus -> return");
            return (HashMap<String, Object>) variant.getDbStatus();
        }
    }

//	private void applyRules(Variant variant) {
//    logggedinUser = new User( getOwner() );
//
//		// Set the correct ApprovalState
//		// The next Approval state is the approval state intens send us
//		// So we have to take the old one (from db)
//    useRequestedOwnerAndGroup == true
//
//    // missing!!!!!
//    Map<String, Object> modifications = elementService.getModifiedProperties(this.toMap());
//    this.setModified(modifications);
//	}

    /**
     * Rename a variant
     *
     * @param id      Id of the variant to rename
     * @param newname New name of the variant
     * @return The renamed variant
     */
    @PutMapping(path = "/variants/{id}/rename")
    @Transactional
    @Operation(security = {
            @SecurityRequirement(name = "bearer-key")
    }
    )
    public Map<String, Object> rename(@PathVariable("id") Long id, @RequestBody String newname, @RequestParam(required = false, defaultValue = "rename") String reason) {

        Map<String, Object> v = findById(id);
        if (v == null) {
            throw new IllegalArgumentException("Variant " + id + " not found");
        }
        Variant variant = baseEntityFactory.createVariant(v, null);
        String oldName = variant.getName();


        // strip leading and trailing quotation chars
        newname = newname.substring(1, newname.length() - 2);
        final Integer anyRev = null; // any revision instead of variant.getRevision()
        final String anyOwner = null;

        if (
            !findByNameAndRevAndProjId(anyOwner,
                newname, anyRev, variant.getProjId()
            ).isEmpty() ||
            !findByNameAndVisibility(newname).isEmpty()
        ) {
            logger.error("Name {} for variant already exists", newname);
            throw new IntensWsException("A variant with this name already exists.",
                    HttpStatus.BAD_REQUEST);
        }

        variant.setName(newname);
        // check permission
        final Long anyId = null;
        final int maxRevision = getMaxRevision(VARIANT_TYPE, variant.getName(),
                                               variant.getProjId(), anyOwner, anyId);
        Owner owner = getOwner();
        variant.applyRules(false, elementService, new User(owner), kieSession, maxRevision);

        if (!variant.isValid()) {
            logger.error("Error in validation {}", variant.getError());
            throw new IntensWsException("Error in validation:\n" + variant.getError(),
                    HttpStatus.BAD_REQUEST);
        }

        logger.debug("Variant: permission check for user {} passed", getUser());
        logger.debug("Variant {}", variant.toMap().toString());

        try {
            var allVariants = findByNameAndRevAndProjId(anyOwner,
                oldName, anyRev, variant.getProjId());
            for (var currentVariantMap : allVariants) {
                var currentVariant = findById((Long) currentVariantMap.get("id"));
                currentVariant.put("name", newname);
                elementService.save(currentVariant, VARIANT_TYPE, getUser(), reason);
            }
            return findById(variant.getId());
        } catch (CoreException e) {
            logger.error("Could not save new name of variant \n{}", e.getMessage());
            throw new IntensWsException(e, HttpStatus.BAD_REQUEST);

        } catch (UsernameNotFoundException e) {
            throw new IntensWsException("Owner not found: " + getUser(), HttpStatus.BAD_REQUEST);
        }

    }

    /**
     * Delete a variant
     *
     * @param id Id of the variant to delete
     */
    @DeleteMapping(path = "/variants/{id}")
    @Transactional
    @Operation(security = {
            @SecurityRequirement(name = "bearer-key")
    }
    )
    public void delete(@PathVariable("id") Long id) {
        if (id == null) {
            throw new IntensWsException("Variant Id is null", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> v = findById(id);
        if (v == null) {
            throw new IntensWsException("Variant " + id + " not found", HttpStatus.BAD_REQUEST);
        }
        Variant variant = baseEntityFactory.createVariant(v, null);
        // check owner
        Owner owner = getOwner();
        if (!owner.getUsername().equals(variant.getOwner().getUsername())) {
            throw new IllegalArgumentException("Owner of variant " + variant.getName() + " is not you.");
        }
        // check approval state
        if (variant.getApprovalState() != ApprovalState.InPreparation &&
                variant.getApprovalState() != ApprovalState.Experimental) {
            throw new IntensWsException("Illegal approval state " + variant.getApprovalState(),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            elementService.deleteElement(id);

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
    @PutMapping(path = "/variants/import")
    @Transactional
    @Operation(security = {
      @SecurityRequirement(name = "bearer-key")
    }
    )
    public Map<String, Object> create(@RequestBody Map<String, Object> compdata) {
        Variant comp = null;
        String ownername = null;
        try {
          // missing owner -> return BAD_REQUEST
          if (!compdata.containsKey("owner") || ((String) compdata.get("owner")).isEmpty()) {
            logger.warn("No owner found");
            throw new IntensWsException("No owner found", HttpStatus.BAD_REQUEST);
          }
          ownername = (String) compdata.get("owner");
          Owner owner = userService.findOwnerByUsername(ownername);
          comp = baseEntityFactory.createVariant(compdata, owner);

          comp.setId(null);
          String reason = compdata.containsKey("reason") ? (String) compdata.get("reason") : "imported";
          return elementService.save(comp.toMap(), VARIANT_TYPE, owner.getUsername(), reason);

        } catch (IllegalArgumentException e) {
          throw new IntensWsException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (CoreException e) { // up-to-date conflict
          throw new IntensWsException(
              "<b>" + e.getMessage() + "</b><br/>" + comp + "<br/>",
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
     * Partial update of a variant. You can send a Dict only with these data you want to update in the
     * variant.
     *
     * @param data The data you want update
     * @param id   The id of the variant you want to update
     * @return The updated variant
     */
    @Transactional
    @PutMapping(path = "/variants/{id}")
    @Operation(security = {
            @SecurityRequirement(name = "bearer-key")
    }
    )
    public Map<String, Object> update(@RequestBody HashMap<String, Object> data,
                                      @PathVariable("id") Long id) {
        //HashMap<String, Object> status = new HashMap<String, Object>();
        logger.debug("Start partial update of variant with id {}", id);
        logger.debug("GROUP 0 {}", data.get("group"));

        // Load the variant to get to projectID
        logger.debug("Step 1 of 9: Load variant by id {}", id);
        Map<String, Object> variant_from_db = findById(id);
        if (variant_from_db == null) {
            logger.error("FAIL step 1 of 9: No variant found with id: {}", id);
            throw new IntensWsException("A variant with id: " + id + " not found", HttpStatus.BAD_REQUEST);
        }

        if (data.isEmpty() || data == null) {
            return variant_from_db;
        }

        logger.debug("GROUP 0 {}", data.get("group"));
        logger.info("Update variant");

        // Every variant must have a projectId otherwise the save function would have aborted
        Long projectId = (Long) variant_from_db.get("projectId");

        //* * * * * * * * * * * * * * * * * * * *
        // 2 of 9 Check if name exists
        //* * * * * * * * * * * * * * * * * * * *
        logger.debug(
                "Step 2 of 9: If name shall be changed, check if new name is allowed (does not exist)");
        // If name is present and the name is changed -> check if new name is allowed
        String origName = null;
        boolean checkIfNameExists = false;
        String name = (String) variant_from_db.get("name");
        if (data.containsKey("name")) {
            data.put("name", stripQuotationChars((String) data.get("name")));
            if (!variant_from_db.get("name").equals(data.get("name"))) {
                origName = name;  // needed to rename other components
                logger.debug("Name is changed {} -> {}", variant_from_db.get("name"), data.get("name"));
                name = (String) data.get("name");  // new name
                checkIfNameExists = true;
            }
        }
        if (componentProperties.useOwnerNameRevisionAsIdentifier &&  // same name allowed (experimental, rev 0)
            !checkIfNameExists &&  // name did not change
            data.containsKey("approval") &&  // new approval provided
            !variant_from_db.get("approval").equals(data.get("approval")) &&  // approval changed
            variant_from_db.get("approval").equals(ApprovalState.Experimental.getName())  // approval was experimental
            ) {
            logger.debug("Approval is changed {} -> {}", variant_from_db.get("approval"), data.get("approval"));
            checkIfNameExists = true;
        }

        if(checkIfNameExists) {
            String owner = null;
            if (componentProperties.useOwnerNameRevisionAsIdentifier) {
                owner = (String) variant_from_db.get("owner");
            }
            if (getMaxRevision(VARIANT_TYPE, name, projectId, owner, id) >= 0) {
                logger.error("FAIL: 2 of 9: Name {} already exists", name);
                throw new IntensWsException("Variant with this name already exists.",
                                            HttpStatus.BAD_REQUEST);
                // Note: removed name from error message as requested by Alstom (Andrey K.)
            }
        }  // finish checking name

        logger.debug("GROUP {}", data.get("group"));
        //* * * * * * * * * * * * * * * * * * * *
        // 3 of 9 Apply new data get modification
        //* * * * * * * * * * * * * * * * * * * *
        logger.debug("Step 3 of 9: apply new data and get modifications");
        // updated_variant is now fully loaded from database with the new
        // attributes:
        Map<String, Object> updated_variant = updateElementValues(data, id);
        Variant variant = baseEntityFactory.createVariant(updated_variant, null);
        // check if createVariant gives an error
        if (variant.getDbStatus().get("status").equals("error")) {
            logger.warn("create variant failed -> status: {}", variant.getDbStatus());
            throw new IntensWsException((String) variant.getDbStatus().get("text"), HttpStatus.BAD_REQUEST);
        }

        variant.getDbStatus().put("status", "update");

        logger.debug("Created variant: {}", variant.toMap().toString());

        String reason = variant.checkReason(data);  // If no reason was sent the reason is null

        Map<String, Object> modifications = elementService.getModifiedProperties(variant.toMap());

        if (modifications.isEmpty()) {
            logger.info("Variant not modified");
            return variant_from_db;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Variant is modified:");
            for (String key : modifications.keySet()) {
                logger.debug("  {} -> {}", key, modifications.get(key).toString());
            }
        }

        variant.getDbStatus().put("id", variant.getId());
        variant.setModified(modifications);

        // loaded variant from db => variant_from_db
        String app = (String) variant_from_db.get("approval");

        //* * * * * * * * * * * * * * * * * * * *
        // 4 of 9 Check Approval transition
        //* * * * * * * * * * * * * * * * * * * *
        if (!variant.isInteractiveReason() &&
            app != null && updated_variant.containsKey("approval")) {
            logger.debug("Step 4 of 9: Check Approval State transition");
            ApprovalState from = ApprovalState.get(app);
            logger.debug("Variant: check approval state transition from {} to {}",
                    from, variant.getApprovalState());
            if (!approvalStateTransition.isValid(from, variant.getApprovalState())) {
                variant.getDbStatus().put("status", "error");
                variant.getDbStatus().put("text", "Variant invalid state transition: " +
                        from + " --> " + variant.getApprovalState());
                logger.error("Step 4 of 9 Failed: Variant invalid state transition {} -> {}", from,
                        variant.getApprovalState());
                // changed as requested by Alstom
                throw new IntensWsException("Invalid state transition.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        //* * * * * * * * * * * * * * * * * * * *
        // 5 of 9 Apply rules
        //* * * * * * * * * * * * * * * * * * * *
        logger.debug("Variant modified? {}", variant.isModified());
        logger.debug("Variant approval state: {}", variant.getApprovalState());
        logger.info("Variant: approval state transition check: passed");
        logger.debug("Step 5 of 9: ApplyRules");

        final String anyOwner = null;
        final Long anyId = null;
        final int maxRevision = getMaxRevision(VARIANT_TYPE, variant.getName(),
                                               variant.getProjId(), anyOwner, anyId);
        variant.applyRules(true, elementService, new User(getOwner()), kieSession, maxRevision);

        if (variant.getId() == null) { // eventually a new variant?
            variant.getDbStatus().put("status", "new");
            variant.getDbStatus().remove("rev");
            logger.error("Step 5 of 9 failed: New variant -> Error in Update");
            throw new IntensWsException("Variant is new after applyRules in update",
                    HttpStatus.BAD_REQUEST);
        }

        logger.debug("Variant: rules check: {}", variant.isValid());
        if (!variant.isValid()) {
            variant.getDbStatus().put("status", "error");
            variant.getDbStatus().put("text", "permission denied. Cannot update variant: "
                    + variant + "\nUser:" + getUser());
            logger.error("Step 5 of 9 failed: Permission denied");
            throw new IntensWsException(variant.getError(), HttpStatus.BAD_REQUEST);
        }

        //* * * * * * * * * * * * * * * * * * * *
        // 6 of 9 Check if upToDateConflict exists
        //* * * * * * * * * * * * * * * * * * * *
        logger.debug("Step 6 of 9: Check Version");
        if (!elementService.checkVersion(variant.getId(), variant.getVersion(), VARIANT_TYPE)) {
            logger.warn("Step 6 of 9 failed: Version conflict for {} version {}", variant.getId(),
                    variant.getVersion());
            variant.getDbStatus().put("status", "upToDateConflict");
            variant.getDbStatus().put("type", VARIANT_TYPE);
            variant.getDbStatus().put("id", variant.getId());
            variant.getDbStatus().put("name", variant.getName());
            variant.getDbStatus().put("rev", variant.getRevision());
            variant.getDbStatus().put("version", variant_from_db.get("version"));
            // date and user of last modification
            String username = variant.getOwner().getUsername();
            Date changed = variant.getCreated();
            Modification mod = elementService.getLatestModification(variant.getId());
            if (mod != null) {
                username = mod.getUser().getUsername();
                changed = mod.getTimestamp();
            }
            variant.getDbStatus().put("username", getOwner().getFullName());
            Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            variant.getDbStatus().put("changed", formatter.format(changed));
            variant.getDbStatus().put("changer", formatter.format(username));
            logger.debug("UpToDate conflict in variant -> return");
            throw new IntensWsException("Up-to-date conflict", HttpStatus.BAD_REQUEST);
        }

        logger.debug("Variant: up-to-date conflict check: passed");
        logger.debug("variant OK -> return status {}", variant.getDbStatus().toString());

        logger.debug("Step 7 of 9: Check if interactive- /reason is given");
        if (reason == null) {
            logger.debug("Step 7 of 9: Reason missing");
            throw new IntensWsException(variant.getDbStatus(), HttpStatus.CONFLICT);
        } // FINISHED: Step 7: check if reason exists finish

//	Variant v = (Variant) new Variant(updated_variant);
//	logger.debug("Step 8 of 9: Apply rules");
//	  // WHY?!?
//	  v.applyRules(true, elementService, new User(getOwner()), kieSession);
//	if (!v.isValid()) {
//		logger.error("FAIL step 8 of 9: Variant not valid {}", v.toString());
//		throw new IntensWsException("Variant permission denied:\n" + v.toString() + "\n" + getUser(),
//				HttpStatus.BAD_REQUEST);
//	}

        if (variant.isNew()) {
            logger.debug("New Variant");
            variant.setCreated(new Date());
            variant.setNextRevision(maxRevision);
            variant.incRevision();
            // TODO: ApprovalState.Default
            variant.setApprovalState(componentProperties.getDefaultApprovalState(
            		"component", variant.getRevision()==0));
        }

        try {
            logger.debug("GROUP {}", updated_variant.get("group"));
            logger.debug("Step 9 of 9: Save variant");
            Map<String, Object> savedData = elementService.save(
            		variant.toMap(), VARIANT_TYPE, getUser(), reason);
            if(origName == null) {
            	return savedData;
            }
            // rename other revisions of this variant
            List<String> pnames = new ArrayList<>(); // we don't need anything except id 
            boolean latestRevision = true;
            Map<String, Object> searchargs = new HashMap<String, Object>();
            searchargs.put("name", new SearchEq<String>(origName));
            if(componentProperties.useOwnerNameRevisionAsIdentifier) {
                SearchEq<String> searchExp = new SearchEq<>(
              		  ApprovalState.Experimental.getName());
                searchargs.put("_not_approval", searchExp);
            }
            if (projectId != null) {
                searchargs.put("projectId", new SearchEq<Long>(projectId));
            }

            for (Map<String, Object> m : elementService.findByType(VARIANT_TYPE,
                    anyOwner, pnames, searchargs, null, 0, 0, null, latestRevision)) {
          	  if(!variant.getId().equals((Long)m.get("id"))) {
          		  logger.debug("rename {} id {} name {}", 
          				VARIANT_TYPE, m.get("id"), name);
          		  m.put("name", (Object)name);
          		  elementService.save(
              		  m, VARIANT_TYPE, (String)m.get("owner"), "renamed to " + name);
          	  }
            }
            return savedData;
            
        } catch (UsernameNotFoundException e) {
            logger
                    .error("FAIL step 9 of 9: Save not possible, the  username {} was not found", getUser());
            throw new IntensWsException(e, HttpStatus.BAD_REQUEST);

        } catch (CoreException e) {
            logger.error("FAIL step 9 of 9: Save not possible", e);
            throw new IntensWsException(e, HttpStatus.BAD_REQUEST);
        }
    }

}
