package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "name": "LDPEM"
    }
    """)
public class ExGroup {
  public String name;

  private ExGroup() {}
}
