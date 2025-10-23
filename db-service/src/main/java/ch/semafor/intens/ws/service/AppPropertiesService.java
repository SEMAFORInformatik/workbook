package ch.semafor.intens.ws.service;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ch.semafor.intens.ws.config.AppProperties.DashboardProperties;
import ch.semafor.intens.ws.config.AppProperties;

@RestController
@RequestMapping("/")
public class AppPropertiesService {

  @Autowired
  private AppProperties appProperties;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Autowired
  private Environment env;

  @GetMapping(path = "/properties")
  public DashboardProperties getProps() {

    var dashboard = appProperties.getDashboard();
    if (dashboard == null) {
      dashboard = new DashboardProperties();
    }
    dashboard.setNoAuth(Arrays.asList(env.getActiveProfiles()).contains("auth-none"));
    dashboard.setDbUrl(contextPath);
    if (env.matchesProfiles("sharding")) {
      var mode = env.getProperty("app.sharding-mode");
      dashboard.setShardMode(mode);
    }
    return dashboard;

  }

}
