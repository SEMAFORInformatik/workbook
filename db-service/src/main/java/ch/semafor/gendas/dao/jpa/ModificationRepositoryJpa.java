package ch.semafor.gendas.dao.jpa;

import ch.semafor.gendas.model.TableModification;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Profile("jpa")
public interface ModificationRepositoryJpa extends JpaRepository<TableModification, Long> {
    // find modifications for an element, paged
    @Query("select m from TableModification m where m.element.id = ?1")
    List<TableModification> getModificationsOfElement(Long elementId, Pageable pageable);

    List<TableModification> findByCommentNotLike(String comment);
    List<TableModification> findByCommentLike(String comment);
}
