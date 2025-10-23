package ch.semafor.intens.ws.model.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {
      "_projection": "(attr0, attr1...)",
      "_ignorecase": true,
      "_sort": "(attr0:1,attr1:-1...)",
      "_page": <n>,
      "_pageSize": <pageSize>,
      "id": "id or {id1,id2...}",
      "name": "string",
      "maxAge": years,
      "approval": "(app0,app1)",
      "status": "(s0,s1...)",
      "owner": "ownername",
      "pname": "{val1,val2...} or [l,u] or [l,u) or (l, u], or (l,u)"
    }
    """)
public class ExQueryParams {
  public String _projection;
  public boolean _ignorecase;
  public String _sort;
  public int _page;
  public int _pageSize;
  public String id;
  public String name;
  public int maxAge;
  public String approval;
  public String status;
  public String owner;
  public String pname;

  private ExQueryParams() {}
}
