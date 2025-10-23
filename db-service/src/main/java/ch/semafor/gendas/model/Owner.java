package ch.semafor.gendas.model;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "owners")
@Document("owners")
public class Owner implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @org.springframework.data.annotation.Id
    // @GeneratedValue
    // private Long id;
    // @Column(nullable = false, length = 50, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String totpSecret;
    private boolean totpEnabled;

    private boolean enabled;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "owners_roles", joinColumns = {@JoinColumn(name = "owner_id")}, inverseJoinColumns = @JoinColumn(name = "role_id"))
    // @DBRef
    private Set<Role> roles = new HashSet<Role>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "owners_groups", joinColumns = {@JoinColumn(name = "owner_id")}, inverseJoinColumns = @JoinColumn(name = "group_id"))
    // @DBRef
    private Set<Group> groups = new HashSet<Group>();
    @ManyToOne(cascade = CascadeType.MERGE)
    private Group activeGroup;

    public Owner() {
    }

    public Owner(String username) {
        this.username = username;
    }

    // public Long getId() {
    // return id;
    // }
    //
    // public void setId(Long id) {
    // this.id = id;
    // }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role r) {
        this.roles.add(r);
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public void addGroup(Group group) {
        this.groups.add(group);
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

  /*public void setActiveGroup(String active){
	  if (active != null) {
	      for (Group g : this.groups) {
	        if (active.equals(g.name)) {
	          this.activeGroup = g;
	          return;
	        }
	      }
	    }
	    this.activeGroup = null;
  }
  */

    public Group getActiveGroup() {
        return this.activeGroup;
    }

    public void setActiveGroup(Group active) {
        if (active != null) {
            for (Group g : this.groups) {
                if (active.equals(g)) {
                    this.activeGroup = g;
                    return;
                }
            }
        }
        this.activeGroup = null;
    }

    @Transient
    public String getFullName() {
        if (firstName == null || lastName == null) {
            return this.username;
        }
        return this.firstName + ' ' + this.lastName;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Owner user)) {
            return false;
        }

        return !(username != null ? !username.equals(user.getUsername()) : user
                .getUsername() != null);

    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (username != null ? username.hashCode() : 0);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE)
                .append("username", this.username).append("enabled", this.enabled);

        if (roles != null) {
            sb.append("Granted Authorities: ");

            int i = 0;
            for (Role role : roles) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(role.toString());
                i++;
            }
        } else {
            sb.append("No Granted Authorities");
        }
        if (groups != null) {
            sb.append("Groups: ");

            int i = 0;
            for (Group g : groups) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(g.toString());
                i++;
            }
        } else {
            sb.append("No Groups");
        }
        return sb.toString();
    }

}
