package ch.semafor.intens.ws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private String name;
  private String version;
  private Migration migration;
  private DashboardProperties dashboard;
  private Boolean useOauthGroups = true;
  private String oauthUserField = "preferred_username";
  private String oauthDefaultGroup;

  public String getOauthDefaultGroup() {
    return oauthDefaultGroup;
  }
  public void setOauthDefaultGroup(String oauthDefaultGroup) {
    this.oauthDefaultGroup = oauthDefaultGroup;
  }
  public String getOauthUserField() {
    return oauthUserField;
  }
  public void setOauthUserField(String oauthUserField) {
    this.oauthUserField = oauthUserField;
  }
  public Boolean getUseOauthGroups() {
    return useOauthGroups;
  }
  public void setUseOauthGroups(Boolean useOauthGroups) {
    this.useOauthGroups = useOauthGroups;
  }
  public String getName() {
    return name;
  }
  public String getVersion() {
    return version;
  }
  public Migration getMigration() {
    return migration;
  }
  public DashboardProperties getDashboard() {
    return dashboard;
  }

  public void setName(String name) {
    this.name = name;
  }
  public void setVersion(String version) {
    this.version = version;
  }
  public void setMigration(Migration migration) {
    this.migration = migration;
  }
  public void setDashboard(DashboardProperties dashboard) {
    this.dashboard = dashboard;
  }

    @Bean
    OpenAPI customOpenAPI() {
    return new OpenAPI()
    .components(new Components()
      .addSecuritySchemes("bearer-key",
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
  }

    @Bean
	public
    PasswordEncoder encoder() {
      return new BCryptPasswordEncoder(11);
  }


  public static class Migration {

    private Map<String, String> git;
    private String masterFile;
    private static final String directory = "migrations";

    public String getUsername() { return git.get("username"); }
    public String getToken() { return git.get("token"); }
    public String getUrl() { return git.get("url"); }
    public Map<String, String> getGit() {
      return git;
    }
    public String getMasterFile() { return masterFile; }
    public String getDirectory() {return directory; }

    public void setGit(Map<String, String> git) {
      this.git = git;
    }
    public void setMasterFile(String masterFile) {
      this.masterFile = masterFile;
    }

  }

  public static class DashboardProperties {
    private String dbUrl;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String clientId;
    private String dashboardUrl;
    private String shardMode;
    private boolean useOauthGroups;
    private boolean noAuth;
    private List<String> oauthAdditionalClaims;

    public boolean isUseOauthGroups() {
      return useOauthGroups;
    }
    public void setUseOauthGroups(boolean useOauthGroups) {
      this.useOauthGroups = useOauthGroups;
    }
    public String getDbUrl() {
      return dbUrl;
    }
    public void setDbUrl(String dbUrl) {
      this.dbUrl = dbUrl;
    }
    public String getAuthorizationEndpoint() {
      return authorizationEndpoint;
    }
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
      this.authorizationEndpoint = authorizationEndpoint;
    }
    public String getTokenEndpoint() {
      return tokenEndpoint;
    }
    public void setTokenEndpoint(String tokenEndpoint) {
      this.tokenEndpoint = tokenEndpoint;
    }
    public String getClientId() {
      return clientId;
    }
    public void setClientId(String clientId) {
      this.clientId = clientId;
    }
    public String getDashboardUrl() {
      return dashboardUrl;
    }
    public void setDashboardUrl(String dashboardUrl) {
      this.dashboardUrl = dashboardUrl;
    }
    public String getShardMode() {
      return shardMode;
    }
    public void setShardMode(String shardMode) {
      this.shardMode = shardMode;
    }
    public boolean getNoAuth() {
      return noAuth;
    }
    public void setNoAuth(boolean noAuth) {
      this.noAuth = noAuth;
    }
    public List<String> getOauthAdditionalClaims() {
      return oauthAdditionalClaims;
    }
    public void setOauthAdditionalClaims(List<String> oauthAdditionalClaims) {
      this.oauthAdditionalClaims = oauthAdditionalClaims;
    }

  }


}
