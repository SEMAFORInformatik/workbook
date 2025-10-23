package ch.semafor.intens.ws.service;

import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.service.UserService;
import ch.semafor.intens.ws.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Profile("auth-jwt")
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {
  @Autowired
  UserService userService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Owner owner=null;
    try {
      owner = userService.findOwnerByUsername(username);
    } catch (ch.semafor.gendas.exceptions.UsernameNotFoundException e) {
      throw new UsernameNotFoundException(username);
    }
    UserDetails d = new User(owner);
    return d;
  }
}
