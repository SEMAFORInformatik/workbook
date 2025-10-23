package ch.semafor.gendas.dao.jpa;

import ch.semafor.gendas.model.ElementType;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;


@Profile("jpa")
public interface ElementTypeRepositoryJpa extends JpaRepository<ElementType, Long> {

    /**
     * Find an ElementType by his name
     *
     * @param name Name of the ElementType
     * @return ElementType obj
     */
    @Nullable
    ElementType findByName(final String name);
}
