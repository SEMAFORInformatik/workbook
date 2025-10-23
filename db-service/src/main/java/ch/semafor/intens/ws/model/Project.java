package ch.semafor.intens.ws.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.semafor.intens.ws.config.ComponentProperties;

public class Project extends BaseEntity {
	private static final Logger logger = LoggerFactory.getLogger(Project.class);

	public enum Status {
		Global("global"), 
		Local("local"),
		Division("division"),
		Delivered("delivered"), 
		Obsolete("obsolete");
		
		private final String name;
		
		Status(String name) {
			this.name = name;
		}

		private static final Map<String, Status> lookup = 
				new HashMap<String, Status>();
		static {
			for (Status a : EnumSet.allOf(Status.class))
				lookup.put(a.getName(), a);
		}

		public String getName() {
			return name;
		}

		public static Status get(String name) {
			Status a = lookup.get(name);
			if( a==null){
				a=Status.Global;
			}
			return a;
		}
	}

	public Project( Map<String, Object>m, ComponentProperties componentProperties ){
		super(componentProperties);
		properties = new HashMap<String, Object>(m);
		if( !m.containsKey("name"))
			throw new IllegalArgumentException("Missing name of project");

		if( m.containsKey("type"))
			properties.remove("type");

		setDefaultProperties();

		logger.debug("Project from map, {}", this);
	}

    public Status getDefaultStatus( String type ) {
        // set properties (eg. defaultApprovalState)
        String status = Status.Global.getName(); // Status.get returns default status Global
        final String key = type + ".defaultStatus";
        String value = componentProperties.getProperty(key);
        if (value != null) {
            status = value;
        }
        logger.debug("loadResources: status {} ({})", status, Status.get(status));
        return Status.get(status);
    }

	protected void setDefaultProperties() {
		super.setDefaultProperties();
		properties.remove("rev");
		if( ! properties.containsKey("status") ){
			properties.put("status", getDefaultStatus("project").getName());
		}		
	}
	
	public Status getStatus(){
		return Status.get((String)(properties.get("status")));
	}
	public void setStatus( Status s){
		properties.put("status", s.getName());
	}

	public String toString(){
        String s = getName() +
          " status:" + getStatus().getName();
        if( hasOwner() ){
          s += " owner:" + getOwner().getUsername();
        }
        return s;
	}
}
