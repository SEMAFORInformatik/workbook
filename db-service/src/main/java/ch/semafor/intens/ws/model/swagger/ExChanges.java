package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "changes": 5
    }
    """)
public class ExChanges {
  public String changes;
  
  private ExChanges() {}

}
