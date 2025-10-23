package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "owner": "max",
      "ownername": "Max Mustermann",
      "created": "2016-03-22T09:42:50.794+00:00",
      "approval": "experimental",
      "name": "Variant1",
      "id": 1253726,
      "version": 9,
      "projectId": 1253481,
      "group": "Semafor"
    }
    """)
public class ExVariant {
  public String owner;
  public String ownername;
  public String created;
  public String approval;
  public String name;
  public int id;
  public int version;
  public int projectId;
  public String group;

  private ExVariant() {}
}
