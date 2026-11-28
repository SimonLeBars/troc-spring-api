package fr.cours.troc.user;

import fr.cours.troc.item.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository users;
    private final ItemRepository items;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, ItemRepository items, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.items = items;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return users.findAll();
    }

    public User findById(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable"));
    }

    // Séance 2 (TP 5) : l'inscription. Le mot de passe n'est jamais enregistré, seulement son haché.
    public User register(RegisterRequest request) {
        if (users.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'identifiant " + request.username() + " est déjà pris");
        }
        User user = new User(); // id null : attribué par la base
        user.setName(request.name());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return users.save(user);
    }

    public User update(Long id, Long currentUserId, UpdateUserRequest request) {
        checkOwnAccount(id, currentUserId); // 403
        User user = findById(id);           // 404 (compte supprimé depuis la connexion)
        user.setName(request.name());       // le reste, dont le haché, est conservé
        return users.save(user);
    }

    public void delete(Long id, Long currentUserId) {
        checkOwnAccount(id, currentUserId);
        findById(id);
        // Séance 3 : la clé étrangère items.owner_id refuserait la suppression (500).
        // Choix : le signaler proprement par un 409 plutôt que supprimer ses objets en cascade.
        if (items.countByOwnerId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'utilisateur " + id + " possède encore des objets");
        }
        users.deleteById(id);
    }

    // 403 avant 404, par choix : sinon, un 404 sur /users/42 dirait à n'importe quel utilisateur
    // connecté si le compte 42 existe.
    private void checkOwnAccount(Long id, Long currentUserId) {
        if (!Objects.equals(id, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un utilisateur ne peut modifier ou supprimer que son propre compte");
        }
    }
}
