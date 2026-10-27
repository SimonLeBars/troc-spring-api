package fr.cours.troc.user;

import org.springframework.data.repository.ListCrudRepository;

// Séance 3 : plus d'implémentation à écrire, Spring Data JDBC la génère au démarrage.
// save, findAll, findById, deleteById… sont fournies par ListCrudRepository.
public interface UserRepository extends ListCrudRepository<User, Long> {
}
