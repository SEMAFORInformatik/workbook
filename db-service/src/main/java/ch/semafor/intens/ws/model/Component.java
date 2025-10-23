package ch.semafor.intens.ws.model;

import ch.semafor.gendas.service.ElementService;
import ch.semafor.intens.ws.config.ComponentProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Component extends BaseEntity {

  private static final Logger logger = LoggerFactory.getLogger(Component.class);
  private String type;
  final static String TYPE_ATTRIBUTE = "type";

  /**
   * create a component
   *
   * @param m map with properties
   * @throws IllegalArgumentException if type or name is missing
   */
  public Component(Map<String, Object> m, ComponentProperties componentProperties) {
    this("unknown", componentProperties);

    properties = new HashMap<String, Object>(m);

    if (!properties.containsKey(TYPE_ATTRIBUTE)) {
      throw new IllegalArgumentException("Type not found");
    }

    type = (String) properties.get(TYPE_ATTRIBUTE);
    properties.remove(TYPE_ATTRIBUTE);

    if (!properties.containsKey("name")) {
      throw new IllegalArgumentException("Missing name of " + type + " component");
    }

    setDefaultProperties();
    logger.debug("Component from map, {}", this);
  }

  // create an empty component that only has a status
  public Component(String status, ComponentProperties componentProperties) {
    super(componentProperties);
    dbStatus.put("status", status);
    logger.debug("Empty Component with status {}", status);
  }

  protected void setDefaultProperties() {
    super.setDefaultProperties();
    if (!properties.containsKey("approval")) {
      properties.put("approval",
    		  componentProperties.getDefaultApprovalState(
    				  "component",
    				  getRevision() == 0).getName());
    }
  }

  public String getType() {
    return type;
  }

  public String toString() {
    String s = getName() + " rev:" + getRevision() +
        " (" + getApprovalState().getName() + ")";
    if (hasOwner()) {
      s += "<br/>Owner:" + getOwner().getUsername();
    }
    if (getGroup() != null) {
      s += " Group:" + getGroup().getName();
    }
    return s;
  }

  public void applyRules(Boolean useRequestedOwnerAndGroup, ElementService elementService,
      User loggedinUser, StatelessKieSession kieSession, int maxRevision,
      String mode) {
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
    cmds.add(CommandFactory.newInsert(mode, "mode"));
    cmds.add(CommandFactory.newInsert(this, "comp"));
    cmds.add(CommandFactory.newInsert(this.getGroup(), "group"));
    cmds.add(CommandFactory.newInsert(loggedinUser, "user"));
    ExecutionResults results = kieSession.execute(CommandFactory.newBatchExecution(cmds));

    // above super.applyRules(...) set comp approvalState to old approvalState
    if (this.check_newRevisionFromObsolete("component", this.getApprovalState(),
        this.getNextApprovalState())) {
      // trigger to create a new revision (below)
      this.setId(null);
    }

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
      this.getDbStatus()
          .put("text", this.getError() + "<br/>" + this.getType() + " " + this);
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

    //    logger.debug("check up-to-date conflict (optimistic locking)");
    // --> moved to checkStatus, save of ComponentService
//        try {
//            checkVersion(this, this.getDbStatus(), this.getType());
//        } catch (UsernameNotFoundException e) {
//            throw new IntensWsException(this.getDbStatus(), HttpStatus.BAD_REQUEST);
//        }
  }

  public Map<String, Object> getProperties() {
    return this.properties;
  }

}
