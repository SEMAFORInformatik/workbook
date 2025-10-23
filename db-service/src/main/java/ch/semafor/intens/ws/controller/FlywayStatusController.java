package ch.semafor.intens.ws.controller;

import ch.semafor.intens.ws.config.AppProperties;
import ch.semafor.intens.ws.model.SchemaVersion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Profile("flyway")
@RequestMapping("/services/rest/flyway")
public class FlywayStatusController {

  @Autowired
  AppProperties properties;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Secured("ROLE_ADMIN")
  @GetMapping("/status")
  @ResponseBody
  public Map<String, Object> getStatus() {
    List<SchemaVersion> info;
    try {
      info = this.jdbcTemplate.query(
              "select \"description\", \"success\", \"installed_on\", \"version\", \"success\" from \"schema_version\" order by \"version\"",
              new SchemaVersionMapper());
    } catch (Exception ex) {
      info = new ArrayList<SchemaVersion>();
      SchemaVersion v = new SchemaVersion();
      v.setDescription("no schema version found.");//+ex.getMessage());
      info.add(v);
    }
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("appVersion", properties.getVersion());
    result.put("schemaVersion", info);
    return result;
  }

  // helper class
  private static final class SchemaVersionMapper implements RowMapper<SchemaVersion> {
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public SchemaVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
      SchemaVersion v = new SchemaVersion();
      v.setDescription(rs.getString("description"));
      v.setVersion(rs.getString("version"));
      v.setInstalled_on(df.format(rs.getDate("installed_on")));
      v.setSuccess(rs.getBoolean("success"));
      return v;
    }
  }
}
