package ch.semafor.gendas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a user role
 *
 * @author tar
 */
@Entity
@Table(name = "roles")
public class Role implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    //@GeneratedValue
    //Long id;
    String name;
    String description;

    public Role() {
    }

    public Role(String authority) {
        this.name = authority;
    }

    //	public Long getId() {
//		return id;
//	}
//	public void setId(Long id) {
//		this.id = id;
//	}
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Role role)) {
            return false;
        }

        return Objects.equals(name, role.name);

    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("name", name);
        return m;
    }

    public String toString() {
        return this.name;
    }
}
