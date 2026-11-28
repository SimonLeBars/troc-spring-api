package fr.cours.troc.user;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

// Séance 3 : plus d'implémentation à écrire, Spring Data JDBC la génère au démarrage.
// save, findAll, findById, deleteById… sont fournies par ListCrudRepository.
public interface UserRepository extends ListCrudRepository<User, Long> {

    // SELECT … FROM users WHERE username = ? : au plus une ligne, grâce à UNIQUE (username)
    Optional<User> findByUsername(String username);
}
