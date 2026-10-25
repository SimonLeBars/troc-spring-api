package fr.cours.troc.user;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

// Évolution 2 : un stockage en mémoire qui affiche chaque opération dans la console.
// Pour que Spring l'utilise à la place d'InMemoryUserRepository, il suffit de déplacer @Repository.
public class LoggingUserRepository implements UserRepository {

    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final String prefix;

    // Évolution 3 : le préfixe des logs est choisi par celui qui crée le repository.
    // Séance 2 : Spring le lit dans application.properties, [USERS] si la clé est absente.
    public LoggingUserRepository(@Value("${troc.user.log-prefix:[USERS]}") String prefix) {
        this.prefix = prefix;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(nextId.getAndIncrement());
        }
        users.put(user.getId(), user);
        System.out.println(prefix + " save " + user.getId() + " " + user.getName());
        return user;
    }

    @Override
    public List<User> findAll() {
        System.out.println(prefix + " findAll");
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findById(Long id) {
        System.out.println(prefix + " findById " + id);
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void deleteById(Long id) {
        System.out.println(prefix + " deleteById " + id);
        users.remove(id);
    }
}
