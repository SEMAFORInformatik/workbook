package ch.semafor.intens.ws.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Role;

/**
 * mapper of Owner for authorization
 * @author tar
 *
 */
public class User implements UserDetails {
	/**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(User.class);
	
	private final Owner owner;
	
	public User( Owner owner ){
		this.owner = owner;
		logger.debug("User {}", owner.getUsername());
		for( Role r: owner.getRoles()){
			logger.debug(" {}",r.getName());
		}
	}
	public User(String username) {
		this.owner=new Owner(username);
	}
	public Map<String, Object> toMap(){
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("username", this.owner.getUsername());
		m.put("firstName", this.owner.getFirstName());
		m.put("lastName", this.owner.getLastName());
		m.put("enabled", this.owner.isEnabled());
		if( this.owner.getActiveGroup()!=null ){
			m.put("activeGroup", this.owner.getActiveGroup().toMap());
		}
		List<Map<String,Object>> roles=new ArrayList<Map<String,Object>>();
		for(Role r: owner.getRoles()){
			roles.add(r.toMap());
		}
		m.put("roles", roles);
		List<Map<String,Object>> groups=new ArrayList<Map<String,Object>>();
		for(Group g: owner.getGroups()){
			groups.add(g.toMap());
		}
		m.put("groups", groups);
		return m;
	}
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<GrantedAuthority>();
        for( Role r: owner.getRoles()){
        	authorities.add(new SimpleGrantedAuthority(r.getName()));
        }
        return authorities;
	}
	
	public void setAuthorities(List<String> authorities) {
		Set<Role> roles = new HashSet<Role>();
		for( String a: authorities){
        	roles.add(new Role(a));
        }
		owner.setRoles(roles);
	}

	@Override
	public String getPassword() {
		return owner.getPassword();
	}
	
	@Override
	public String getUsername() {
		return owner.getUsername();
	}
	

	public String getFirstName(){
		return owner.getFirstName();
	}
	
	public String getLastName(){
		return owner.getLastName();
	}
	
	
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return owner.isEnabled();
	}

	public Set<Group> getGroups(){
		return owner.getGroups();
	}

	public Set<Role> getRoles(){
		return owner.getRoles();
	}

  public boolean isTotpEnabled() {
    return owner.isTotpEnabled();
  }

  public String getTotpSecret() {
    return owner.getTotpSecret();
  }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (owner == null) {
			return other.owner == null;
		} else return owner.equals(other.owner);
	}
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer( owner.getUsername() );
		buf.append(" authorities: ");
		buf.append(getAuthorities());
		return buf.toString();
	}
	public Group getActiveGroup() {
		if( owner!=null){
			return owner.getActiveGroup();
		}
		return null;
	}

	public Owner getOwner() {
		return owner;
	}
}
