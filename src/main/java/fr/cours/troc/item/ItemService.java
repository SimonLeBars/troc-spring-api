package fr.cours.troc.item;

import fr.cours.troc.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository items;
    private final UserRepository users;
    private final ItemProperties props;

    // Évolution 1 : ItemService reçoit le même UserRepository que UserService,
    // donc les utilisateurs créés d'un côté sont bien visibles de l'autre.
    public ItemService(ItemRepository items, UserRepository users, ItemProperties props) {
        this.items = items;
        this.users = users;
        this.props = props;
    }

    public List<Item> findAll(Long ownerId) {
        if (ownerId == null) {
            return items.findAll();
        }
        return items.findByOwnerId(ownerId);
    }

    public Item findById(Long id) {
        return items.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Objet " + id + " introuvable"));
    }

    public Item create(Item item) {
        checkItem(item);
        if (items.countByOwnerId(item.getOwnerId()) >= props.maxPerUser()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'utilisateur " + item.getOwnerId() + " a déjà " + props.maxPerUser() + " objets");
        }
        item.setId(null);
        return items.save(item);
    }

    public Item update(Long id, Item item) {
        findById(id);
        checkItem(item);
        item.setId(id);
        return items.save(item);
    }

    public void delete(Long id) {
        findById(id);
        items.deleteById(id);
    }

    private void checkItem(Item item) {
        if (item.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être positive");
        }
        if (users.findById(item.getOwnerId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Utilisateur " + item.getOwnerId() + " introuvable");
        }
    }
}
