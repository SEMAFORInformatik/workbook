package ch.semafor.gendas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//import org.springframework.data.mongodb.core.mapping.Document;

@Entity
//@Document(collection="groups")
@Table(name = "groups")
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    // Two id annotations are needed to support both mongo and jpa
    @Id
    String name;

    public Group(String name) {
        this.name = name;
    }

    public Group() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Group other)) {
            return false;
        }

        return Objects.equals(name, other.getName());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("name", name);
        return m;
    }

    @Override
    public String toString() {
        return "Group [name=" + name + "]";
    }


}
