package fr.cours.troc.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// Évolution 2 : un stockage en mémoire qui affiche chaque opération dans la console.
// Pour que Spring l'utilise à la place d'InMemoryUserRepository, il suffit de déplacer @Repository.
public class LoggingUserRepository implements UserRepository {

    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final String prefix;

    // Spring ne sait pas quelle String injecter : ce constructeur fournit un préfixe par défaut.
    // En séance 2, le préfixe viendra du fichier de configuration.
    public LoggingUserRepository() {
        this("[USERS]");
    }

    // Évolution 3 : le préfixe des logs est choisi par celui qui crée le repository.
    public LoggingUserRepository(String prefix) {
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
    public User findById(Long id) {
        System.out.println(prefix + " findById " + id);
        return users.get(id);
    }
}
