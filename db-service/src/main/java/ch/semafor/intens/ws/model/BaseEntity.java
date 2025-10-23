package ch.semafor.intens.ws.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.intens.ws.config.ComponentProperties;

public class BaseEntity {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntity.class);
    protected ComponentProperties componentProperties;

    String errmsg = null;

    Date created;

	// The element properties
    protected Map<String, Object> properties = new HashMap<String, Object>();

    // All modifications of the component are stored in this map,
    // which is send back to the user
    Map<String, Object> modifications;

    // Indicates if something has modified
    boolean modified = false;
    // Indicates that the modification is trivial
    boolean trivial = false;

    // The status which is sent back to the client
    protected Map<String, Object> dbStatus = new HashMap<String, Object>();

    // Owner of the component (!Loggedin user)
    User owner = null;

    private Group group;

    // Next states
    private int nextRev = 1;

    private ApprovalState nextApprovalState = ApprovalState.InPreparation;

    private User nextOwner = null;

    private Group nextGroup = null;

    private Boolean interactiveReason = false;

    public BaseEntity(ComponentProperties componentProperties) {
        this.componentProperties = componentProperties;
    }

    protected void setDefaultProperties() {
        // id must be of type Long
        if (properties.containsKey("id")) {
            if (properties.get("id") == null) {
                properties.remove("id");
            } else {
                properties.put("id", Long.valueOf(((Number) properties.get("id")).longValue()));
            }
        }

        if (hasOwner()) {
            owner = new User((String) properties.get("owner"));
        }
        if (hasGroup()) {
            group = new Group((String) properties.get("group"));
        } else if (owner != null) {
            group = owner.getActiveGroup();
            if (group != null) {
                this.properties.put("group", group.getName());
            }
        }
        if (!properties.containsKey("rev")) {
            properties.put("rev", 0);
        }
        if (!properties.containsKey("version")) {
            properties.put("version", 0L);
        }
        if (properties.containsKey("created")) {
            if (properties.get("created") instanceof String) {
                setCreated((String) properties.get("created"));
            } else {
                setCreated((Date) properties.get("created"));
            }
        } else {
            setCreated(new Date());
        }

        // remove interactiveReason and reason from properties. We do not want to store them in the database
        for (String key : Arrays.asList("interactiveReason", "reason")) {
            properties.remove(key);
        }
    }

    public boolean hasGroup() {
        return properties.get("group") != null && !((String) properties.get("group")).isEmpty();
    }

    public void setName(String name) {
        if (name == null) {
            return;
        }
        properties.put("name", name);
    }

    public void setId(Long id) {
        if (id == null) {
            properties.remove("id");
        } else {
            properties.put("id", id);
        }
    }

    public Long getId() {
        return (Long) properties.get("id");
    }

    public Long getVersion() {
        Number num = (Number) properties.get("version");
        if (num == null) {
            return -1L;
        }
        return num.longValue();
    }

    public Map<String, Object> getDbStatus() {
        return dbStatus;
    }

    public void setRevision(int r) {
        properties.put("rev", r);
        logger.debug("Component {}", this);
    }

    public Date getCreated() {
        return created;
    }

    public void incRevision() {
        properties.put("rev", nextRev);
        properties.remove("id"); // this is a new component!
        properties.put("version", 0L); // this is a new component!
        logger.debug("Component {}", this);
    }

    public void setError(String msg) {
        this.errmsg = msg;
    }

    public void setModified(Map<String, Object> modifications) {
        this.modified = false;
        if (modifications.isEmpty()) return;

        getDbStatus().put("status", "modified");
        getDbStatus().put("modifications", modifications);
        this.modifications = modifications;
        this.modified = true;

        if (modifications.containsKey("approval")) {
            getDbStatus().put("approval", modifications.get("approval"));
        }
        logger.debug(" Approval modification {}", modifications.get("approval"));

        // check if this modification is trivial (more than the triv properties modified):
        // (Maybe the property list should be configurable)
        this.trivial = true;
        Set<String> triv = new HashSet<String>(
             Arrays.asList("approval", "name", "desc", "group", "owner", "changer"));
        for (String k : modifications.keySet()) {
            if (!triv.contains(k)) {
                this.trivial = false;
                break;
            }
        }
        getDbStatus().put("substatus", trivial ? "trivial" : "nontrivial");
    }

    public boolean hasOwner() {
        return properties.get("owner") != null &&
                !((String) properties.get("owner")).isEmpty();
    }


    public void setOwner(String name) {
        if (name == null) {
            properties.remove("owner");
            owner = null;
            return;
        }
        properties.put("owner", name);
        owner = new User(name);
    }

    public void setGroup(String name) {
        properties.put("group", name);
        group = new Group(name);
    }

    public void setCreated(Date date) {
        created = date;
        properties.put("created", created);
    }

    public void setCreated(Calendar caldate) {
        setCreated(caldate.getTime());
    }

    public void setCreated(String datestr) {
        if (datestr.indexOf(' ') > 0) { // accept date format: yyyy-MM-dd HH:mm:ss
            datestr = datestr.replace(' ', 'T');
        }
        try {// accept date format rfc 822 and xmldate  (TODO use FastDateFormat)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZ");
            created = sdf.parse(datestr);
        } catch (ParseException pe) {
            try {
                Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime(datestr);
                created = cal.getTime();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid date format " + datestr);
            }
        }

        properties.put("created", created);
    }

    public User getOwner() {
        return owner;
    }

    public Group getGroup() {
        return group;
    }

    public String getName() {
        return (String) properties.get("name");
    }

    @Deprecated
    public void setValid(boolean flag) {
        if (flag) return;
        this.errmsg = "Permission denied.";
    }

    public String getError() {
        return this.errmsg;
    }

    public boolean isOK() {
        return this.errmsg == null;
    }

    public boolean isValid() {
        return isOK();
    }

    public boolean isModified() {
        return this.modified;
    }

    public boolean isTrivial() {
        return this.trivial;
    }

    public int getRevision() {
        try {
            return (Integer) properties.get("rev");
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public Map<String, Object> toMap() {
        return properties;
    }

    public void setNextRevision(int maxRevision) {
        this.nextRev = maxRevision + 1;
    }

    public int getNextRevision() {
        return nextRev;
    }

    public void setNextApprovalState(ApprovalState nextApprovalState) {
        this.nextApprovalState = nextApprovalState;
    }

    public ApprovalState getNextApprovalState() {
        return nextApprovalState;
    }

    public User getNextOwner() {
        return nextOwner;
    }

    public void setNextOwner(String nextOwnerName) {
        if (nextOwnerName == null) {
            this.nextOwner = null;
            return;
        }

        this.nextOwner = new User(nextOwnerName);
    }

    public Group getNextGroup() {
        return nextGroup;
    }

    public void setNextGroup(String nextGroup) {

        this.nextGroup = new Group(nextGroup);
    }

    public Boolean isInteractiveReason() {
        return interactiveReason;
    }

    public void setInteractiveReason(Boolean interactiveReason) {
        this.interactiveReason = interactiveReason;
    }

    public boolean isUpToDateConflict() {
        String status = (String) getDbStatus().get("status");
        if (status == null) {
            return false;
        }
        return status.equals("upToDateConflict");
    }

    public boolean isNew() {
        String status = (String) getDbStatus().get("status");
        if (status == null) return false;
        return getDbStatus().get("status").equals("new");
    }

    public Map<String, Object> getModifications() {
        return modifications;
    }

    public ApprovalState getApprovalState() {
        return ApprovalState.get((String) (properties.get("approval")));
    }

    public void setApprovalState(ApprovalState a) {

        properties.put("approval", a.getName());
    }

    public String checkReason(Map<String, Object> compdata) {
        /**
         * Get the reason from data
         */

        String reason;

        if (compdata.containsKey("interactiveReason")) {
            reason = (String) compdata.get("interactiveReason");
            logger.debug("Interactive reason found: {}", reason);
            compdata.remove("interactiveReason");
            this.setInteractiveReason(true);
            return reason;
        }

        if (compdata.containsKey("reason")) {
            reason = (String) compdata.get("reason");
            logger.debug("Reason found: {}", reason);
            compdata.remove("reason");
            return reason;
        }
        return null;
    }

    public void applyRules(Boolean useRequestedOwnerAndGroup, ElementService elementService) {
        /*
         * apply rules and checkVersion
         */
        logger.debug("apply rules for {}", this.getId());

//        User user = new User(getOwner());
        // logger.debug("current user " + loggedinUser);

        // Set the next approval, owner and group to next
        this.setNextApprovalState(this.getApprovalState());
        if (useRequestedOwnerAndGroup) {
            logger.debug("Set old owner {}", this.owner.getUsername());
            this.setNextOwner(this.owner.getUsername());
            this.setNextGroup(this.group.getName());
        }

        // Load the component
        Map<String, Object> m = elementService.getElementMap(this.getId());

        // Set the from approval, group, owner

        // Approval-State
        String app = (String) m.get("approval");
        ApprovalState fromApprovalState = ApprovalState.get(app);
        this.setApprovalState(fromApprovalState);

        // Owner
        String fromOwner = (String) m.get("owner");
        logger.debug("from: {}", fromOwner);
        this.setOwner(fromOwner);

        // Group
        String fromGroup = (String) m.get("group");
        this.setGroup(fromGroup);

        // if only approval changed, modifications are empty
        // because of above this.setApprovalState(fromApprovalState)
        Map<String, Object> modifications = elementService.getModifiedProperties(this.toMap());
        this.setModified(modifications);
    }

    public Boolean check_newRevisionFromObsolete(String type, ApprovalState from, ApprovalState to) {
        // check if approval changes from obsolete to something else and
        //       if creating a new revision from an obsolete one by changing the status is allowed
        if (from != ApprovalState.Obsolete || to == ApprovalState.Obsolete) {
            logger.debug("({}, {}, {}): approval does not change from obsolete to something else, return false",
                    type, from.getName(), to.getName());
            return false;
        }

        Boolean allowed = componentProperties.isRequestToChangeApprovalStateFromObsoleteCreatesNewRevision(type);
        logger.debug("({}, {}, {}): {}",
                type, from.getName(), to.getName(), allowed);
        return allowed;
    }

    public void removeProperty(String propname) {

        this.properties.entrySet().removeIf(e -> e.getValue().equals(propname));
    }
}
