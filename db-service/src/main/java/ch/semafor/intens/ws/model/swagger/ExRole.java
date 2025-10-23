package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "name": "ROLE_USER"
    }
    """)
public class ExRole {
  public String name;

  private ExRole() {}
}
