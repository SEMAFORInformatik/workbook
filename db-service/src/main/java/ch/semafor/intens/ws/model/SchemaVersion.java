package ch.semafor.intens.ws.model;

import java.util.Date;

// Flyway Schema Version
public class SchemaVersion {
    Integer version_rank;
    Integer installed_rank;
    String version;
    String description;
    String type;
    String installed_by;
    String installed_on;
    Boolean success;
    
	public Integer getVersion_rank() {
		return version_rank;
	}
	public void setVersion_rank(Integer version_rank) {
		this.version_rank = version_rank;
	}
	public Integer getInstalled_rank() {
		return installed_rank;
	}
	public void setInstalled_rank(Integer installed_rank) {
		this.installed_rank = installed_rank;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getInstalled_by() {
		return installed_by;
	}
	public void setInstalled_by(String installed_by) {
		this.installed_by = installed_by;
	}
	public String getInstalled_on() {
		return installed_on;
	}
	public void setInstalled_on(String installed_on) {
		this.installed_on = installed_on;
	}
	public Boolean getSuccess() {
		return success;
	}
	public void setSuccess(Boolean success) {
		this.success = success;
	}


}
