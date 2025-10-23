package ch.semafor.intens.ws.utils;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Filter project, variant and component lists according to owner and group
 *
 * @author tar
 */
public class AccessFilter {
    private static final Logger logger = LoggerFactory.getLogger(AccessFilter.class);
    private Set<Group> memberOf;
    private String user = null;
    private ComponentProperties componentProperties;
    private Boolean active = false;
    private Boolean isAdmin = false;

    public AccessFilter(Owner user, ComponentProperties componentProperties) {
        this.user = user.getUsername();
        this.memberOf = user.getGroups();
        this.isAdmin = user.getRoles().stream().anyMatch(role -> role.getName().contains("ADMIN"));
        this.componentProperties = componentProperties;
        active = componentProperties.isTrue("AccessFilter.active");
        logger.debug("active {}", active);
    }

    private Boolean isMemberOf(Group group){
        for(Group g : this.memberOf){
            if(g.getName().equals(group.getName()))
                return true;
        }
        return false;
    }
    public Boolean isCreationAllowed(Component c) {
        if (c == null) return true;


        if (!active) {
            logger.debug("allowed: AccessFilter is not active");
            return true;
        }

        if (!c.hasOwner() || user.equals(c.getOwner().getUsername()) || !c.hasGroup()) {
            logger.debug("allowed: no owner or user is creator of component or no group");
            return true;
        }

        logger.debug("Component {} Owner {} Group {}", c.getName(), c.getOwner().getUsername(),
                c.getGroup().getName());

        if (isMemberOf(c.getGroup())) {
            logger.debug("User is member of group");
            // TODO: Use component.approval.group.copy
            if (componentProperties.isTrue("component.approval.group.shared")) {
                logger.debug("allowed (same group)");
                return true;
            }
            logger.debug("denied (same group)");
            return false;
        }

        // not owner and not member of group
        if (componentProperties.isTrue("component.approval.others.shared")) {
            logger.debug("allowed (other group)");
            return true;
        }
        logger.debug("denied (other group)");
        return false;
    }

    public Boolean isCreationAllowed(Map<String, Object> o) {
        if (o == null || o.isEmpty()) return true;
        logger.debug("Component {} Owner {} Group {}", o.get("name"), o.get("owner"), o.get("group"));

        if (!active) {
            logger.debug("allowed: AccessFilter is not active");
            return true;
        }

        if (o.get("owner") == null ||
            user.equals(o.get("owner")) ||
            o.get("group") == null
        ) {
            logger.warn("allowed: no owner or user is creator of component or no group");
            return true;
        }

        Group group = new Group((String) o.get("group"));
        if (isMemberOf(group)) {
            logger.debug("User is member of group");
            // TODO: Use component.approval.group.copy
            if (componentProperties.isTrue("component.approval.group.shared")) {
                logger.debug("allowed (same group)");
                return true;
            }
            logger.debug("denied (same group)");
            return false;
        }

        // not owner and not member of group
        if (componentProperties.isTrue("component.approval.others.shared")) {
            logger.debug("allowed (other group)");
            return true;
        }
        logger.debug("denied (other group)");
        return false;
    }

    private List<Map<String, Object>> filter(String type, String status, List<Map<String, Object>> l) {
        if (!active) {
            logger.debug("AccessFilter is not active, do nothing");
            return l;
        }

        if (isAdmin) {
            logger.debug("allowed: User is admin");
            return l;
        }
        // TODO use Predicate
        Iterator<Map<String, Object>> it = l.iterator();
        while (it.hasNext()) {
            Map<String, Object> m = it.next();
            logger.debug("checking {} of {}", type, m.get("name"));
            boolean allow = true;
            if (!user.equals(m.get("owner"))) {
                Group group = new Group((String) m.get("group"));
                String code = (String) m.get(status);
                if (code != null) {
                    if (isMemberOf(group)) {
                        allow = componentProperties.isTrue(type + "." + status + ".group." + code);
                        logger.debug("User in group {} of {} {}: access allowed {} for status {}",
                                     group.getName(), type, m.get("name"), allow, code);
                    } else {
                        allow = componentProperties.isTrue(type + "." + status + ".others." + code);
                        logger.debug("User not in group {} of {} {}: access allowed {} for status {}",
                                     group.getName(), type, m.get("name"), allow, code);
                    }
                }
            }
            if (!allow) it.remove();
        }
        return l;
    }

    public List<Map<String, Object>> filterModified(List<Map<String, Object>> l, Date changedAfter) {
        logger.debug("filter 'changed' geq {}", changedAfter);
        Iterator<Map<String, Object>> it = l.iterator();
        while (it.hasNext()) {
            Map<String, Object> m = it.next();
            if (m.containsKey("changed")) {
                Date changed = (Date) m.get("changed");
                logger.debug("checking changed = {}", changed);

                if (changed.before(changedAfter)) {
                    logger.debug("--> remove {}", changed);
                    it.remove();
                }
            }
        }
        return l;
    }

    public List<Map<String, Object>> filterComponents(List<Map<String, Object>> l) {
        return filter("component", "approval", l);
    }

    public List<Map<String, Object>> filterVariants(List<Map<String, Object>> l) {
        return filter("variant", "approval", l);
    }

    public List<Map<String, Object>> filterProjects(List<Map<String, Object>> l) {
        return filter("project", "status", l);
    }
}
