package ch.semafor.gendas.dao.jpa;

import ch.semafor.gendas.dao.OwnerRepository;
import ch.semafor.gendas.model.Owner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("jpa")
public interface OwnerRepositoryJpa extends JpaRepository<Owner, String>, OwnerRepository {
}
