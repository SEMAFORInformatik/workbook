package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "username": "max",
      "password": "",
      "firstName": "Max",
      "lastName": "Mustermann",
      "enabled": true,
      "roles": [
        {
          "name": "ROLE_USER",
          "description": null
        }
      ],
      "groups": [
        {
          "name": "Semafor"
        }
      ],
      "activeGroup": {
        "name": "Semafor"
      }
    }
    """)
public class ExUser {
  public String username;
  public String firstName;
  public String lastName;
  public String password;
  public ExRole[] roles;
  public ExGroup[] groups;
  public ExGroup activeGroup;
  public boolean enabled;

  private ExUser() {}
}
