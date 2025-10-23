package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "owner": "max",
      "ownername": "Max Mustermann",
      "created": "2016-02-01T07:34:11.084+00:00",
      "desc": "Kippmomentproblem",
      "name": "Project1",
      "id": 1251832,
      "version": 1,
      "group": "Semafor",
      "status": "global"
    }
    """)
public class ExProject {
  // All fields
  private int id;
  private String name;
  private String owner;
  private String ownername;
  private int version;
  private String group;
  private String created;
  private String desc;
  private String status;

  private ExProject() {}

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getOwnername() {
    return ownername;
  }

  public void setOwnername(String ownername) {
    this.ownername = ownername;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
