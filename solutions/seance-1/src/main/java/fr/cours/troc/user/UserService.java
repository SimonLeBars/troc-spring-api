package fr.cours.troc.user;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User create(User user) {
		return userRepository.save(user);
	}

	public List<User> findAll() {
		return userRepository.findAll();
	}
}
