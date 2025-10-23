package ch.semafor.intens.ws.service;

import java.text.Format;
import java.text.SimpleDateFormat;
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
import java.util.stream.Collectors;

import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Modification;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.PropertyType;
import ch.semafor.gendas.model.PropertyType.Type;
import ch.semafor.gendas.model.Role;
import ch.semafor.gendas.search.SearchEq;
import ch.semafor.gendas.search.SearchGt;
import ch.semafor.gendas.search.SearchIn;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.ApprovalState;
import ch.semafor.intens.ws.model.BaseEntity;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.security.SecurityUtils;
import ch.semafor.intens.ws.utils.DateTimeFormatter;
import ch.semafor.intens.ws.utils.IntensWsException;
import ch.semafor.intens.ws.utils.SearchFactory;

@Transactional
public class BaseServiceImpl {

    static public final String PROJECT_TYPE = "Project";
    static public final String VARIANT_TYPE = "Variant";
    private static final Logger logger = LoggerFactory.getLogger(BaseServiceImpl.class);
    @Autowired
    ElementService elementService;
    @Autowired
    UserService userService;

    @Autowired
    StatelessKieSession kieSession;

    @Autowired
    ComponentProperties componentProperties;
    
    public void setSession(StatelessKieSession s) {
        this.kieSession = s;
    }

    /** returns maximum revision of a component given by type and name
     *  
     * @param type of component (incl. VARIANT_TYPE)
     * @param name of component
     * @param projectId (optional) projectId in case of type variant
     * @param owner (optional) owner name: ignore experimental of others (if not null)
     * @param id (optional) ignore rev of this id (if not null)
     * @return
     */
    public int getMaxRevision(String type, String name, Long projectId,
    		String owner, Long id) {
        int maxrev = -1;
        List<String> pnames = Arrays.asList("rev", "owner", "approval");
        Map<String, Object> searchargs = new HashMap<String, Object>();
        searchargs.put("name", new SearchEq<String>(name));
        if (projectId != null) {
            searchargs.put("projectId", new SearchEq<Long>(projectId));
        }
        logger.debug("comp type {} name {} projectId {} owner {}",
        		type, name, projectId, owner);
        boolean latestRevision = true;
        String anyOwner = null;
        for (Map<String, Object> m : elementService.findByType(type,
                anyOwner, pnames, searchargs, null, 0, 0, null, latestRevision)) {
            logger.debug("{} id {} rev {} owner {} approval {}",
            		type, m.get("id"), m.get("rev"), m.get("owner"), m.get("approval"));
            if (owner != null
            		&& (((String) m.get("approval")).equals(ApprovalState.Experimental.getName())
            				&& !((String) m.get("owner")).equals(owner))) {
            	logger.debug("ignore experimental of other owner");
            	continue;
            }
            if (id != null && ((Long) m.get("id")).equals(id)) {
            	logger.debug("ignore component with given id");
            	continue;
            }
            if (m.get("rev") != null && (Integer) m.get("rev") > maxrev){
                maxrev = (Integer) m.get("rev");
            }
        }
        logger.debug(" --> {}", maxrev);
        return maxrev;
    }

    protected boolean hasAdminRole(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication auth = securityContext.getAuthentication();
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    Map<String, Object> extractSearchArgs(Map<String, String> queryparams) {
        Map<String, Object> search = new HashMap<String, Object>();
        Boolean ignorecase = false;

        if (queryparams.containsKey("id")) {
            String id = queryparams.get("id");
            if ((id.startsWith("(") && id.endsWith(")")) ||
                (id.startsWith("{") && id.endsWith("}"))) {
                String[] ids = id.substring(1, id.length() - 1).split(",");
                Long[] idl = new Long[ids.length];
                int i = 0;
                for (String idstr : ids) {
                    idl[i++] = Long.valueOf(idstr);
                }
                search.put("id", new SearchIn<Long>(idl));
                queryparams.clear(); // ignore all other parameters when id's are given
            }
        }

        if (queryparams.containsKey("_ignorecase")) {
            ignorecase = true;
            if (!queryparams.get("_ignorecase").isEmpty()) {
                ignorecase = Boolean.parseBoolean(queryparams.get("_ignorecase"));
            }
            search.put("ignorecase", ignorecase);
            //NOTE: not a valid searchargs, but needed for sorting, will be removing at the realsearch
            queryparams.remove("_ignorecase");
        }
        if (queryparams.containsKey("name")) {
            String name = queryparams.get("name");
            search.put("name", new SearchEq<String>(name, ignorecase));
            queryparams.remove("name");
        }
        if (queryparams.containsKey("_not_name")) {
            String name = queryparams.get("_not_name");
            search.put("_not_name", new SearchEq<String>(name, ignorecase));
            queryparams.remove("_not_name");
        }
        if (queryparams.containsKey("maxAge")) {
            Integer maxAge = Integer.valueOf(queryparams.get("maxAge"));
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.YEAR, -maxAge);
            search.put("created", new SearchGt<Date>(cal.getTime()));
            queryparams.remove("maxAge");
        }
        if (queryparams.containsKey("approval")) {
            String approval = queryparams.get("approval");
            if (approval.startsWith("(") && approval.endsWith(")")) {
                search.put("approval", new SearchIn<String>(
                        approval.substring(1, approval.length() - 1).split(",")));
            } else {
                search.put("approval", new SearchEq<String>(approval));
            }
            queryparams.remove("approval");
        }
        if (queryparams.containsKey("status")) {
            String status = queryparams.get("status");
            if (status.startsWith("(") && status.endsWith(")")) {
                search.put("status", new SearchIn<String>(
                        status.substring(1, status.length() - 1).split(",")));
            } else {
                search.put("status", new SearchEq<String>(status));
            }
            queryparams.remove("status");
        }
        return search;
    }

    protected String extractOwnerName(Map<String, String> queryparams) {
        String ownername = null;
        if (queryparams.containsKey("owner")) {
            ownername = queryparams.get("owner");
            queryparams.remove("owner");
        }
        return ownername;
    }

    protected List<String> extractFields(Map<String, String> queryparams, List<String> minimal) {
        Set<String> fields = new HashSet<String>();
        fields.addAll(minimal);
        if (queryparams.containsKey("_projection")) {
            String projection = queryparams.get("_projection");
            if (projection.startsWith("(") && projection.endsWith(")")) {
                fields.addAll(Arrays.asList(
                        projection.substring(1, projection.length() - 1).split(",")));
            } else {
                fields.add(projection);
            }
            queryparams.remove("_projection");
        }
        return new ArrayList<String>(fields);
    }


    protected Map<String, Integer> extractSort(Map<String, String> queryparams) {
        Map<String, Integer> sortmap = null;
        // example _sort=name:1
        if (queryparams.containsKey("_sort")) {
            String sortpars = queryparams.get("_sort");
            sortmap = new HashMap<String, Integer>();
            if (sortpars.startsWith("(") && sortpars.endsWith(")")) {
                for (String kvp : sortpars.substring(1, sortpars.length() - 1).split(",")) {
                    String[] k = kvp.split(":");
                    if (k.length == 2) {
                        sortmap.put(k[0], Integer.parseInt(k[1]));
                    }
                }

            } else {
                String[] k = sortpars.split(":");
                if (k.length == 2) {
                    sortmap.put(k[0], Integer.parseInt(k[1]));
                }
            }
            queryparams.remove("_sort");
        }

        return sortmap;
    }

    protected int extractPage(Map<String, String> queryparams) {
        int page = -1;
        if (queryparams.containsKey("_page")) {
            page = Integer.parseInt(queryparams.get("_page"));
            queryparams.remove("_page");
        }
        return page;
    }

    protected int extractPageSize(Map<String, String> queryparams) {
        int pagesize = -1;
        if (queryparams.containsKey("_pageSize")) {
            pagesize = Integer.parseInt(queryparams.get("_pageSize"));
            queryparams.remove("_pageSize");
        }
        return pagesize;
    }

    private void fixDate(List<Map<String, Object>> props, String type) {
        for (Object property : props) {
            if (property instanceof List) {
                fixDate((List<Map<String, Object>>) property, type);
            } else {
                fixDate((Map<String, Object>) property, type);
            }
        }
    }

    public void fixDate(Map<String, Object> props, String type) {
        if (props == null) {
            return;
        }
        ElementType t = elementService.getElementType(type);
        if (t == null) {
            logger.warn("unknown ElementType {}", type); // TODO
            return;
        }
        for (String key : props.keySet()) {
            if (key.equals("type") || key.equals("id") ||
                    key.equals("rev") || key.equals("version") ||
                    key.equals("changer") || key.equals("changed") ||
                    key.equals("owner") || key.equals("group") ||
                    key.equals("reason") || key.equals("interactiveReason")) {
                continue;
            }
            // find property type of param
            PropertyType p = t.getPropertyType(key);
            if (p != null) {
                if (p.getType() == PropertyType.Type.DATE) {
                    if (props.get(key) instanceof java.util.List) {
                        List dateList = (List) props.get(key);
                        for (int i = 0; i < dateList.size(); i++) {
                            if (dateList.get(i) instanceof String) {
                                dateList.set(i, DateTimeFormatter.convert((String) dateList.get(i)));
                            }
                        }
                    } else if (props.get(key) instanceof String) {
                        props.put(key, DateTimeFormatter.convert((String) props.get(key)));
                    }
                }
            } else { // must be a subtype
                ElementType subtype = t.getElementType(key);
                if (subtype != null) {
                    if (props.get(key) instanceof java.util.List) {
                        fixDate((List<Map<String, Object>>) props.get(key), subtype.getName());
                    } else {
                        fixDate((Map<String, Object>) props.get(key), subtype.getName());
                    }
                } else {
                    logger.warn("{} unknown property {}", type, key); // TODO
                }
            }
        }
    }

    protected Collection<String> getVarCompNames(String varcomp) {
        if (varcomp == null || varcomp.isEmpty()) {
            ElementType variantType = elementService.getElementType(VARIANT_TYPE);

            if (variantType == null) {
                throw new IntensWsException("DB setup problem. Unknown Type " + VARIANT_TYPE,
                        HttpStatus.BAD_REQUEST);
            }

            return variantType.getReferences().keySet();
        }

        List<String> varcompset = new ArrayList<String>();
        varcompset.add(varcomp);
        return varcompset;
    }

    private Object getSearchExpression(String name, String value, Boolean ignorecase, Type type) {
        try { // construct query param object from string
            return SearchFactory.createSearchObject(value, type, ignorecase);
        } catch (Exception ex) {
            logger.error("Illegal Conversion for param {} = {}", name,
                    value);
            throw new IntensWsException(
                    "Type Conversion error for param " + name
                            + " = " + value,
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * optimistic locking check
     *
     * @param entity     to be checked
     * @param status     modification status
     * @param entityType type of entity
     * @throws UsernameNotFoundException if user not found
     */
    public void checkVersion(BaseEntity entity, Map<String, Object> status, String entityType)
            throws UsernameNotFoundException {
        if (entity.getId() == null) {
            return;
        }

        logger.info("checkVersion: {}, {}, {}", entity.getId(), entity.getVersion(), entityType);
        if (elementService.checkVersion(entity.getId(), entity.getVersion(), entityType)) {
            logger.debug("{}: up-to-date conflict check: passed", entity.getName());
            return;
        }
        logger.warn("{}: up-to-date conflict check: failed", entity.getName());
        Map<String, Object> m = elementService
                .getElement(entity.getId(), Collections.singletonList("version"));
        logger.warn("Version conflict for id {} version {} <--> {}",
                entity.getId(), entity.getVersion(), m.get("version"));
        status.put("status", "upToDateConflict");
        status.put("type", entityType);
        status.put("name", entity.getName());
        status.put("rev", entity.getRevision());
        status.put("version", ((Number) m.get("version")).longValue());
        // date and user of last modification
        Date changed = entity.getCreated();
        String username = entity.getOwner().getUsername();
        Modification mod = elementService.getLatestModification(entity.getId());
        if (mod != null) {
            username = mod.getUser().getUsername();
            changed = mod.getTimestamp();
        }
        status.put("username", userService.findOwnerByUsername(username).getFullName());
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        status.put("changed", formatter.format(changed));
    }

    protected String getUser(){
        return SecurityUtils.getCurrentUserLogin();
    }

    public Owner getOwner() {
        // jwt only mode
        Object principal = SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        logger.debug("Principal class {}", principal.getClass().getName());
        if(principal instanceof User){
            logger.debug("getOwner: return First");
            return ((User)principal).getOwner();
        }

        // oauth2 mode
        Map<String, Object> principalAttrs = SecurityUtils.getPrincipalAttributes();
        if( principalAttrs.containsKey("given_name")){
            Collection<SimpleGrantedAuthority> authorities = (Collection<SimpleGrantedAuthority>)
                SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            Owner currentUser = new Owner((String) principalAttrs.get("preferred_username"));
            currentUser.setFirstName((String) principalAttrs.get("given_name"));
            currentUser.setLastName((String) principalAttrs.get("family_name"));
            currentUser.setRoles(authorities.stream().map(SimpleGrantedAuthority::getAuthority).map(Role::new).collect(Collectors.toSet()));
            if(principalAttrs.containsKey("groups")) {
                for (String name : (List<String>) principalAttrs.get("groups")) {
                    logger.debug("Group {}", name);
                    // must remove first char as it is "/"
                    currentUser.addGroup(new Group(name.substring(1, name.length())));
                }
            }
            if(principalAttrs.containsKey("active_group")) {
                String name = (String) principalAttrs.get("active_group");
                currentUser.setActiveGroup(new Group(name.charAt(0) == '/' ? name.substring(1) : name));
            }
            else { // user must have an active group, take the first
                if(!currentUser.getGroups().isEmpty()) {
                    Group activeGroup = currentUser.getGroups().iterator().next();
                    currentUser.setActiveGroup(activeGroup);
                }
            }
            currentUser.setPassword(new BCryptPasswordEncoder(11).encode(currentUser.getUsername()));
            currentUser.setEnabled(true);
            SecurityUtils.setPrincipal(new User(currentUser));
            return userService.saveOwner(currentUser);
        }

        // probably basic auth (deprecated)
        try {
            Owner owner = userService.findOwnerByUsername(
                    getUser());
            SecurityUtils.setPrincipal(new User(owner));
            return owner;
        } catch (UsernameNotFoundException ex) {
            throw new IntensWsException(ex, HttpStatus.BAD_REQUEST);
        }
    }

    public ElementService getElementService() {
        return elementService;
    }

    /**
     * extract JPA/Mongo search arguments from URL query parameters
     *
     * @param type        name of component type
     * @param queryparams map of http query parameters to be extracted
     * @param search      map of search arguments build from query parameters
     * @param childsearch map of search arguments for child components
     * @return true if all params can be found
     */
    protected boolean buildSearch(String type,
                                  Map<String, String> queryparams,
                                  Map<String, Object> search,
                                  Map<String, Map<String, Object>> childsearch) {

        Boolean ignorecase = false;
        if (search.containsKey("ignorecase")) {
            ignorecase = (Boolean) search.get("ignorecase");
        }

        ElementType t = elementService.getElementType(type);
        if (t == null) {
            logger.error("ElementType {} not found", type);
            throw new IntensWsException("ComponentType " + type + " not found",
                    HttpStatus.BAD_REQUEST);
        }
        for (String key : queryparams.keySet()) {
            // find property type of param
            if (key.indexOf(".") == -1) { // simple property of this components -> no '.' found
                PropertyType p = t.getPropertyType(key);
                if (p == null) {
                    logger.warn("Property {} of {} missing", key, type);
                    return false;
                }
                search.put(key, getSearchExpression(key, queryparams.get(key), ignorecase, p.getType()));
            }
            // Childargs
            else {
                String[] sub = key.split("\\.");
                logger.debug("subcomponent(refname) {} property {}", sub[0], sub[1]);
                ElementType reftype = t.getElementTypeOfReference(sub[0]);
                if (reftype == null) {
                    logger.warn("reftype {} of {} missing", sub[0], type);
                    return false;
                }
                PropertyType p = reftype.getPropertyType(sub[1]);
                if (p == null) {
                    logger.warn("Property {} of {} missing", sub[1], type);
                    return false;
                }
                if (childsearch.get(sub[0]) == null) {
                    childsearch.put(sub[0], new HashMap<String, Object>());
                }
                childsearch.get(sub[0]).put(sub[1], getSearchExpression(sub[1],
                        queryparams.get(key), ignorecase, p.getType()));
            }
        }
        return true;
    }

    protected String stripQuotationChars(String newname) {
        if (newname.charAt(0) != '"') {
            return newname;
        }
        int l = newname.lastIndexOf('"');
        if (l > 0) {
            return newname.substring(1, l);
        }
        return newname.substring(1);
    }

    protected Map<String, Object> updateElementValues(HashMap<String, Object> data, Long id) {
        // Clean up
        data.remove("id");
        // Created at
        data.remove("created");
        // Revision
        data.remove("rev");
        // Version TODO: check for up-to-date conflict
        data.remove("version");

        // Get the element
        boolean withDbInfos = false;
        Map<String, Object> m = elementService.getElementMap(id, withDbInfos);
        logger.debug("Get Element Map: {}", m.toString());
        m.remove("ownername");
        logger.debug("Update element with id {}", id);
        logger.debug("Size of update: {}", data.size());
        for (String key : data.keySet()) {
          Boolean invalid = false;
          try {
            invalid = data.get(key).toString().equals("");
          } catch (NullPointerException ex) {
            invalid = true;
          }
          if (invalid) {
            m.remove(key);
            // Do not put the key
            continue;
          }
            // Check if user is in group
          Owner o = getOwner();
		  // If user is admin, don't filter projects
          if (!this.hasAdminRole()) {
            if (key.equals("group")) {
                if (!o.getGroups().contains(new Group((String) data.get(key)))) {
                    logger.error("User {} is not in given group {} -> return", o.getFullName(), data.get(key));
                    throw new IntensWsException("<b>Update Permission Denied:</b> User not in group '" + data.get(key) + "'",
                            HttpStatus.BAD_REQUEST);
                }

            }
         }
            logger.debug("Add {} with value {}", key, data.get(key));
            m.put(key, data.get(key));
        }
        return m;
    }

}
