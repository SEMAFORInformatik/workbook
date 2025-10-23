package ch.semafor.gendas.dao.jpa;

import ch.semafor.gendas.dao.GroupRepository;
import ch.semafor.gendas.model.Group;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("jpa")
public interface GroupRepositoryJpa extends JpaRepository<Group, String>, GroupRepository {
}
