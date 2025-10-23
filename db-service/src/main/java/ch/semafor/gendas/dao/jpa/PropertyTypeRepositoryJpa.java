package ch.semafor.gendas.dao.jpa;

import ch.semafor.gendas.model.ElementType;
import ch.semafor.gendas.model.PropertyType;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

@Profile("jpa")
public interface PropertyTypeRepositoryJpa extends JpaRepository<PropertyType, Long> {

    /**
     * Find PropertyType by his name
     *
     * @param name Name of the PropertType
     * @return PropertyType obj
     */
    @Nullable
    PropertyType findByName(final String name);

    /**
     * Find PropertyType by his ElementType and his Name
     *
     * @param type ElementType of the PropertyType
     * @param name Name of the PropertyType
     * @return PropertyType obj
     */
    @Query("SELECT pt FROM ElementType et JOIN et.propertyTypes pt "
            + "WHERE et=?1 AND pt.name = ?2")
    PropertyType findByElementTypeAndName(final ElementType type, final String name);

    @Nullable
    PropertyType findByNameAndUnit(String name, String unit);
}
