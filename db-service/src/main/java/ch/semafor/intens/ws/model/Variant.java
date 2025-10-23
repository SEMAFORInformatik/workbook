package ch.semafor.intens.ws.model;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.text.WordUtils;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.service.VariantsService;

public class Variant extends BaseEntity {
    private static final Logger logger = LoggerFactory.getLogger(Variant.class);

    public Variant(Map<String, Object> m, ComponentProperties componentProperties) {
        this("unknown", componentProperties);
        properties = new HashMap<String, Object>(m);
        if (!m.containsKey("name"))
            throw new IllegalArgumentException("Missing name of variant");

        // Variant can not have an other type as variant
        if (m.containsKey("type"))
            properties.remove("type");

        setDefaultProperties();
        logger.debug("Variant from map, {}", this);
    }

    // create an empty variant that only has a status
    public Variant(String status, ComponentProperties componentProperties) {
        super(componentProperties);
        this.dbStatus.put("type", VariantsService.VARIANT_TYPE);
        this.dbStatus.put("status", status);
        logger.debug("Empty Variant with status {}", status);
    }

    protected void setDefaultProperties() {
        super.setDefaultProperties();
        if (!properties.containsKey("approval")) {
            properties.put("approval",
            		componentProperties.getDefaultApprovalState(
            				"component",
            				getRevision() == 0).getName());
        }
        if (!properties.containsKey("rev")) {
            properties.put("rev", 0);
        }
    }

    public Long getProjId() {
        Number num = (Number) this.properties.get("projectId");
        if (num == null)
            return null;
        return Long.valueOf(num.longValue());
    }

    public ApprovalState getApprovalState() {
        return ApprovalState.get((String) (properties.get("approval")));
    }

    public void setApprovalState(ApprovalState a) {
        properties.put("approval", a.getName());
    }

    public String toString() {
        String s = getName() + " rev:" + getRevision() +
                " approvalState:" + getApprovalState().getName();
        if (hasOwner()) {
            s += " owner:" + getOwner().getUsername();
        }
        return s;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    private String getComponentTypeFromName(String name){
        // build component type from name:
        // induction_motor --> InductionMotor
        StringBuffer typename = new StringBuffer();
        for( String s: name.split("_")){
            typename.append(WordUtils.capitalize(s));
        }
        return typename.toString();
    }
    public List<Map.Entry<BigInteger, String>> getComponents() {
        /**
         * This method gets all components which are connected to it.
         * This is useful when you approve a variant and have to check
         * if all components (which are connected) have the same level of approval.
         * @returns list of pair(key, type)
         */
        var components = new ArrayList<Map.Entry<BigInteger, String>>();
        var header = new HashSet<String>(Arrays.asList("name", "rev",
                "created", "owner", "ownername", "group", "approval", "id", "projectId", "version"));

        // Loop through all properties and check if this property is not a head property
        for (String key : properties.keySet()) {
            if (header.contains(key)) {
                // This is a head property
                continue;
            }
            // This is not a head property -> Check if this variant property has an attribute compId
            if (properties.get(key) instanceof List<?>) {
                logger.debug("checking compid for variant property key {}", key);
                for (Map<String, Object> m : (List<Map<String, Object>>) properties.get(key)) {
                    if (m.containsKey("compId")) {
                        BigInteger compid = BigInteger.valueOf(
                                ((Number) m.get("compId")).longValue());
                        // The type always starts with a Capitalize Letter e.g model -> Model, pmMotor -> PmMotor
                        String ctype = this.getComponentTypeFromName(key);
                        logger.debug("  compId {}, Type {}", compid, ctype);
                        components.add(new AbstractMap.SimpleEntry<>(compid, ctype));
                    }
                }
                continue;
            }

            // Check if we have a Map with a compId
            if (properties.get(key) instanceof Map<?, ?>) {
                if (((Map<String, Object>) properties.get(key)).containsKey("compId")) {
                    BigInteger compid = BigInteger.valueOf(
                            ((Number) ((Map<String, Object>) properties.get(key)).get("compId")).longValue());
                    var c = new HashMap<BigInteger, String>();
                    String ctype = this.getComponentTypeFromName(key);
                    logger.debug("  compId {}, Type {}", compid, ctype);
                    components.add(new AbstractMap.SimpleEntry<>(compid, ctype));
                }
            }

        }
        return components;

    }

    public void applyRules(Boolean useRequestedOwnerAndGroup, ElementService elementService,
                           User loggedinUser, StatelessKieSession kieSession, int maxRevision) {
        if (this.getId() == null) {
            logger.warn("No id found for apply Rules -> return");
            return; // nothing to check as this comp is new
        }
        if (this.dbStatus.containsKey("status") && this.dbStatus.get("status").equals("error")) {
            logger.warn("Component has an error -> Apply rules will not continue");
            return;
        }
        logger.debug("Apply Rules with user {}", loggedinUser);
        super.applyRules(useRequestedOwnerAndGroup, elementService);

        this.setNextRevision(maxRevision);
        logger.debug("maxRevision : {}", maxRevision);

        List<Command> cmds = new ArrayList<Command>();
        cmds.add(CommandFactory.newInsert(this, "variant"));
        cmds.add(CommandFactory.newInsert(this.getGroup(), "group"));
        cmds.add(CommandFactory.newInsert(loggedinUser, "user"));
        ExecutionResults results = kieSession.execute(CommandFactory.newBatchExecution(cmds));

        // restore approval, owner and group
        this.setApprovalState(this.getNextApprovalState());
        if (useRequestedOwnerAndGroup) {
            this.setOwner(this.getNextOwner().getUsername());
            this.setGroup(this.getNextGroup().getName());
        }

        // apply rules set an error
        if (!this.isValid()) { // error is set
            logger.warn("apply rules failed! {}", this.getError());
            this.getDbStatus().put("status", "error");
            this.getDbStatus().put("text", this.getError() + "<br/>" + VariantsService.VARIANT_TYPE + " " + this);
            return;
        }
        logger.debug("apply rules passed!");

        // apply rules deleted id -> create a new component
        if (this.getId() == null) {
            logger.debug("apply rules deleted id -> create a new component");
            this.getDbStatus().put("status", "new");
            this.getDbStatus().remove("rev");
            this.getDbStatus().remove("id");

            if (modifications == null) {
                modifications = new HashMap<String, Object>();
            }
            modifications.put("rev", this.getNextRevision());

            return;
        }
    }
}
