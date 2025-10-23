package ch.semafor.intens.ws.model.swagger;

public class ExCheckStatus {
  private String name;
  private int id;
  private String text;
  private String type;
  private String status;
  
  private ExCheckStatus() {}

  public String getName() {
    return name;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setName(String name) {
    this.name = name;
  }

}
