package ch.semafor.intens.ws.utils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ch.semafor.gendas.model.Owner;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.Component;
import ch.semafor.intens.ws.model.Variant;
import ch.semafor.intens.ws.service.VariantsService;

@org.springframework.stereotype.Component
public class BaseEntityFactory {
	private static final Logger logger = LoggerFactory.getLogger(BaseEntityFactory.class);

  @Autowired
  ComponentProperties componentProperties;

	// create component and set its status
	public Component createComponent(Map<String, Object> compdata, Owner owner) {
		// a valid component must have a property other than
		// "type", "id", "rev", "version", "owner", "group", "reason", "interactiveReason"
		Set<String> keylist = compdata == null ? new HashSet<String>()
				: new HashSet<String>(compdata.keySet());

		// Check if the component has more than type, id, rev and version
		for (String key : Arrays.asList("type", "id", "rev", "version",
                                        "owner", "group", "reason", "interactiveReason")) {
			keylist.remove(key);
		}
		// empty component is not modified
		if (keylist.isEmpty()) {
			return new Component("notModified", componentProperties);
		}

		Component comp = null;
		try {
			comp = new Component(compdata, componentProperties);
		} catch (IllegalArgumentException e) {
			logger.error("Could not create component");
			comp = new Component("error", componentProperties);
			comp.getDbStatus().put("text", e.getMessage());
			return comp;
		}

		// add id, type, name and rev to status
		for (String key : Arrays.asList("type", "name", "id", "rev")) {
			if (compdata.containsKey(key))
				comp.getDbStatus().put(key, compdata.get(key));
		}

		// set owner and group if missing
		if (!compdata.containsKey("owner")) {
			logger.debug("Have no owner -> use logged in user ({})", owner.getUsername());
			comp.setOwner(owner.getUsername());
		}

		if (!compdata.containsKey("group")) {
			if (owner.getActiveGroup() == null) {
				throw new IntensWsException("No active group", HttpStatus.BAD_REQUEST);
			}
			logger.debug("Have no group -> use default");
			comp.setGroup(owner.getActiveGroup().getName());
		}
		logger.debug("Owner: {}. Group {}", comp.getOwner().getUsername(), comp.getGroup().getName());
		return comp;
	}

	public Variant createVariant(Map<String, Object> compdata, Owner owner) {
		// a valid variant must have a property other than
		// "type", "id", "rev", "version", "owner", "group", "reason", "interactiveReason"
		Set<String> keylist = compdata == null ? new HashSet<String>()
				: new HashSet<String>(compdata.keySet());

		// Check if the component has more than type, id, rev and version
		for (String key : Arrays.asList("type", "id", "rev", "version",
                                        "owner", "group", "reason", "interactiveReason")) {
			keylist.remove(key);
		}
		// empty variant is not modified
		if (keylist.isEmpty()) {
			return new Variant("notModified", componentProperties);
        }

		Variant variant = null;
		try {
			variant = new Variant(compdata, componentProperties);
		} catch (IllegalArgumentException e) {
			logger.error("Could not create variant");
			variant = new Variant("error", componentProperties);
			variant.getDbStatus().put("text", e.getMessage());
			return variant;
		}

        // Add type to status
        variant.getDbStatus().put("type", VariantsService.VARIANT_TYPE);

		// add id, name and rev to status
		for (String key : Arrays.asList("name", "id", "rev")) {
			if (compdata.containsKey(key)) {
				variant.getDbStatus().put(key, compdata.get(key));
			}
		}

		// set owner and group if missing
		if (!compdata.containsKey("owner")) {
			logger.warn("Have no owner -> logged in user");
			variant.setOwner(owner.getUsername());
		}

		if (!compdata.containsKey("group")) {
			if (owner.getActiveGroup() == null) {
				throw new IntensWsException("No active group", HttpStatus.BAD_REQUEST);
			}
			logger.debug("Have no group -> use default");
			variant.setGroup(owner.getActiveGroup().getName());
		}
		logger.debug("Owner: {}. Group {}", variant.getOwner().getUsername(), variant.getGroup().getName());
		return variant;
	}
}
