package fr.cours.troc.user;

import fr.cours.troc.item.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final ItemRepository items;

    public UserService(UserRepository users, ItemRepository items) {
        this.users = users;
        this.items = items;
    }

    public List<User> findAll() {
        return users.findAll();
    }

    public User findById(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable"));
    }

    public User create(User user) {
        user.setId(null); // l'id est attribué par le repository, pas par le client
        return users.save(user);
    }

    public User update(Long id, User user) {
        findById(id); // 404 si l'utilisateur n'existe pas
        user.setId(id);
        return users.save(user);
    }

    public void delete(Long id) {
        findById(id);
        // Séance 3 : la clé étrangère items.owner_id refuserait la suppression (500).
        // Choix : le signaler proprement par un 409 plutôt que supprimer ses objets en cascade.
        if (items.countByOwnerId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'utilisateur " + id + " possède encore des objets");
        }
        users.deleteById(id);
    }
}
