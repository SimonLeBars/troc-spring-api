package fr.cours.troc.item;

import fr.cours.troc.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository items;
    private final UserRepository users;

    // Évolution 1 : ItemService reçoit le même UserRepository que UserService,
    // donc les utilisateurs créés d'un côté sont bien visibles de l'autre.
    public ItemService(ItemRepository items, UserRepository users) {
        this.items = items;
        this.users = users;
    }

    public List<Item> findAll() {
        return items.findAll();
    }

    public Item create(Item item) {
        if (users.findById(item.getOwnerId()) == null) {
            throw new IllegalArgumentException("Utilisateur introuvable : " + item.getOwnerId());
        }
        return items.save(item);
    }
}
