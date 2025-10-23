package ch.semafor.gendas.dao.mongo.custom;

import ch.semafor.gendas.exceptions.CoreException;
import ch.semafor.gendas.model.Group;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

@Profile("mongo")
public interface ElementRepositoryMongoCustom {

    Map<String, Object> save(String type, Map<String, Object> map) throws CoreException;

    void deleteElement(final Long id, final String type);

    void checkVersion(Long id, Long version, String collection) throws CoreException;

    Long findVersion(Long id, String collection);

    Map<String, Object> findById(Long id, String type);

    Map<String, Object> findById(Long id, List<String> pnames, String type);

    List<Map<String, Object>> findByGroup(Group group);

    List<Group> findAllGroups();

    List<Map<String, Object>> find(final String type, final String owner, final java.util.List<String> pnames,
                                   final Map<String, Object> searchargs,
                                   final Map<String, Map<String, Object>> childargs,
                                   Pageable pageable);
}
