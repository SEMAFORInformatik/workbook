package ch.semafor.gendas.service;

import ch.semafor.gendas.dao.OwnerRepository;
import ch.semafor.gendas.dao.mongo.custom.GroupRepositoryMongoCustom;
import ch.semafor.gendas.exceptions.GroupExistsException;
import ch.semafor.gendas.exceptions.UsernameNotFoundException;
import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Profile("mongo")
@Service("userService")
@Transactional(readOnly = true)
public class UserServiceMongo implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceMongo.class);

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private GroupRepositoryMongoCustom groupRepository;

    @Autowired
    private ElementService elementService;

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public Owner findOwnerByUsername(String username) throws UsernameNotFoundException {
        Owner owner = ownerRepository.findByUsername(username);
        if (owner == null) {
            throw new UsernameNotFoundException(username);
        }
        return owner;
    }

    @Override
    @Transactional
    public Owner saveOwner(Owner owner) {
        return ownerRepository.save(owner);
    }

    @Override
    public List<Owner> findOwners(String search) {
        return ownerRepository.findByUsernameLike(search);
    }

    @Override
    public List<Group> getGroups(boolean onlyInUsers) {
        if (onlyInUsers) {
            return groupRepository.findAll();
        }
        Set<Group> groups = new HashSet<>();
        groups.addAll(groupRepository.findAll());
        groups.addAll(elementService.getAllGroups());
        return new ArrayList<>(groups);
    }

    @Override
    @Transactional
    public int renameGroup(String actual, String newName) throws GroupExistsException {
        logger.debug("Rename {} to {}", actual, newName);
        int changes = 0;
        Group newGroup = createGroup(newName);
        Group oldGroup = groupRepository.findByName(actual);

        List<Owner> owners = ownerRepository.findAllByOrderByUsernameAsc();
        logger.debug("Update owners: Found {}.", owners.size());
        for (Owner owner : owners) {
            Set<Group> ownersgroups = owner.getGroups();
            logger.debug("Get groups {} for owner {}", ownersgroups.size(), owner.getFullName());

            if (ownersgroups.contains(oldGroup)) {
                logger.debug("Remove owner {} from old group and add to new group", owner.getFullName());
                ownersgroups.remove(oldGroup);
                ownersgroups.add(newGroup);

                try {
                    if (owner.getActiveGroup().getName().equalsIgnoreCase(oldGroup.getName())) {
                        owner.setActiveGroup(newGroup);
                    }
                } catch (Exception e) {
                    logger.error("No active group found");
                }

                ownerRepository.save(owner);
                changes++;
            }
        }

        changes += elementService.changeGroupInElements(oldGroup, newGroup);

        logger.debug("Changed {} objects", changes);
        return changes;
    }

    private Group createGroup(String groupname) throws GroupExistsException {
        if (groupRepository.findByName(groupname) == null) {
            return new Group(groupname);
        }
        throw new GroupExistsException("Group name " + groupname + " already exists");
    }
}
