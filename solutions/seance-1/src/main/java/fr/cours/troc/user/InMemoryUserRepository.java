package fr.cours.troc.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {

	private final Map<Long, User> users = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong();

	@Override
	public User save(User user) {
		if (user.getId() == null) {
			user.setId(sequence.incrementAndGet());
		}
		users.put(user.getId(), user);
		return user;
	}

	@Override
	public List<User> findAll() {
		return new ArrayList<>(users.values());
	}

	@Override
	public User findById(Long id) {
		return users.get(id);
	}
}
