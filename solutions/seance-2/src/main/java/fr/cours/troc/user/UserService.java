package fr.cours.troc.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> findAll() {
		return userRepository.findAll();
	}

	public User findById(Long id) {
		return userRepository.findById(id).orElseThrow(() ->
				new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable"));
	}

	public User create(User user) {
		user.setId(null); // l'id est attribué par le repository, jamais par le client
		return userRepository.save(user);
	}

	public User update(Long id, User user) {
		findById(id); // 404 si l'utilisateur n'existe pas
		user.setId(id); // l'id vient du chemin, pas du corps
		return userRepository.save(user);
	}

	// Ses objets éventuels restent, avec un ownerId qui ne désigne plus personne :
	// au TP 5, la clé étrangère de la base refusera cette suppression.
	public void delete(Long id) {
		findById(id);
		userRepository.deleteById(id);
	}
}
