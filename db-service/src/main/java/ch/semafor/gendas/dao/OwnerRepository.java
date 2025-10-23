package ch.semafor.gendas.dao;

import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Owner;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface OwnerRepository extends CrudRepository<Owner, String> {

    /**
     * Gets owner based on username.
     *
     * @param username the user's username
     * @return owner object found in database
     */
    Owner findByUsername(String username) throws UsernameNotFoundException;

    /**
     * Gets a list of owners which are like the submitted username
     *
     * @param username the user's username or a part of it
     * @return List populated list of owners
     */
    List<Owner> findByUsernameLike(String username);

    /**
     * Gets a list of owners ordered by the uppercase version of their username.
     *
     * @return List populated list of owners
     */
    List<Owner> findAllByOrderByUsernameAsc();

}
