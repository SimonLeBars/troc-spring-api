package fr.cours.troc.item;

import fr.cours.troc.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

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
        checkNameAvailable(item, null);
        item.setId(null);
        return items.save(item);
    }

    public Item update(Long id, Item item) {
        findById(id);
        checkItem(item);
        checkNameAvailable(item, id);
        item.setId(id);
        return items.save(item);
    }

    public void delete(Long id) {
        findById(id);
        items.deleteById(id);
    }

    // Séance 3 : déplace une quantité d'un objet vers un autre utilisateur.
    // Pas de @Transactional ici : la méthode rejoint la transaction de l'appelant (TradeService.accept).
    public void transfer(Item item, int quantity, Long recipientId) {
        item.setQuantity(item.getQuantity() - quantity); // débit
        items.save(item);

        // crédit : la ligne (destinataire, nom) existe déjà, ou on la crée.
        // Choix : max-per-user n'est pas appliqué ici, un troc accepté ne doit pas échouer pour ça.
        Item credited = items.findByOwnerIdAndName(recipientId, item.getName()).orElseGet(() -> {
            Item created = new Item();
            created.setOwnerId(recipientId);
            created.setName(item.getName());
            return created;
        });
        credited.setQuantity(credited.getQuantity() + quantity);
        items.save(credited);
    }

    // Séance 3 : la contrainte UNIQUE (owner_id, name) refuserait le doublon (500) : on renvoie un 409.
    private void checkNameAvailable(Item item, Long id) {
        items.findByOwnerIdAndName(item.getOwnerId(), item.getName())
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "L'utilisateur " + item.getOwnerId() + " a déjà un objet « " + item.getName() + " »");
                });
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
