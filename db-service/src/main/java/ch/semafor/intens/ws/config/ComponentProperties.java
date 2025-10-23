package ch.semafor.intens.ws.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.semafor.intens.ws.model.ApprovalState;

@Component
public class ComponentProperties {
  private static final Logger logger = LoggerFactory.getLogger(ComponentProperties.class);

  @Value("${component.useNameRevisionAsIdentifier:false}")
  public boolean useNameRevisionAsIdentifier;
  @Value("${component.useOwnerNameRevisionAsIdentifier:false}")
  public boolean useOwnerNameRevisionAsIdentifier;

  @Autowired
  private Environment env;

  public String getProperty(String key) {
    return env.getProperty(key);
  }

  public Boolean isTrue(String key) {
    // return true if key exists and has value 'true' or 'T'
    // return false otherwise
    return isTrue(key, false);
  }

  public Boolean isFalse(String key) {
    // return true if key exists and has value other than 'true' or 'T'
    // return false otherwise
    return !isTrue(key, true);
  }

  public Boolean isNotTrue(String key) {
    // return false if key exists and has value 'true' or 'T'
    // return true otherwise
    return !isTrue(key, false);
  }

  public Boolean isNotFalse(String key) {
    // return false if key exists and has value other than 'true' or 'T'
    // return true otherwise
    return isTrue(key, true);
  }

  private Boolean isTrue(String key, Boolean defaultValue) {
    // return true if key exists and has value 'true' or 'T'
    // return false if key exists and has value other than 'true' or 'T'
    // return default otherwise
    String value = env.getProperty(key);
    if (value == null) {
      logger.debug("key {} not found, return default {}", key, defaultValue);
      return defaultValue;
    }
    return value.equals("T") || value.equals("true");
  }

  public ApprovalState getDefaultApprovalState(String type, boolean rev0) {
	String appState = null;
	if(rev0) {
	    final String key = type + ".defaultApprovalStateRev0";
	    appState = env.getProperty(key);
		logger.debug("default appState rev 0: {} ({})", appState, ApprovalState.get(appState));
	}
	if(appState == null) {
		final String key = type + ".defaultApprovalState";
		appState = env.getProperty(key);  // ApprovalState.get returns default state if argument is null
		logger.debug("default appState: {} ({})", appState, ApprovalState.get(appState));
	}
    return ApprovalState.get(appState);
  }

  public Boolean isSingleNonApprovedRevision(String elementType) {
    // set properties (eg. singleNonApprovedRevision)

    final String key = elementType + ".singleNonApprovedRevision";
    String value = env.getProperty(key);
    if (value == null) {
      logger.debug("isSingleNonApprovedRevision({}): key {} not found, return false", elementType, key);
      return false;
    }

    return value.equals("T") || value.equals("true");
  }

  public Boolean isRequestToChangeApprovalStateFromObsoleteCreatesNewRevision(String type) {
    // is request to change approval state from obsolete to something else accepted
    // and creates a new revision?
    // type: "component"
    final String key = type + ".requestToChangeApprovalStateFromObsoleteCreatesNewRevision";
    String value = env.getProperty(key);

    if (value == null) {
      logger.debug("({}): key {} not found, return false", type, key);
      return false;
    }

    return value.equals("T") || value.equals("true");
  }
}
