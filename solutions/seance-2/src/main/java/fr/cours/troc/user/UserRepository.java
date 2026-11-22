package fr.cours.troc.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

	User save(User user);

	List<User> findAll();

	Optional<User> findById(Long id); // vide si l'utilisateur n'existe pas

	void deleteById(Long id);
}
