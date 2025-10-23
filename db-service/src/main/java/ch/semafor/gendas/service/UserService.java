package ch.semafor.gendas.service;

import ch.semafor.gendas.exceptions.GroupExistsException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;

import java.util.List;

public interface UserService {
    /**
     * find owner by user name
     *
     * @param username
     * @return owner (null if not found)
     * @throws UsernameNotFoundException
     */
    Owner findOwnerByUsername(String username) throws UsernameNotFoundException;

    /**
     * save or modify owner
     *
     * @param owner
     * @return owner
     */
    Owner saveOwner(Owner owner);

    /**
     * find owners
     */
    List<Owner> findOwners(String search);

    /**
     * get groups
     */
    List<Group> getGroups(boolean onlyInUsers);

    /**
     * renames a group
     *
     * @param actual  - The name of the actual group
     * @param newName - The name of the new group
     * @return how many user changed
     */
    int renameGroup(String actual, String newName) throws GroupExistsException;

}
