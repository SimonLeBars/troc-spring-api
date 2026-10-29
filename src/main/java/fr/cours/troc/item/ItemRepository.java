package fr.cours.troc.item;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends ListCrudRepository<Item, Long> {

    // requêtes déduites du nom de la méthode : WHERE owner_id = ?
    List<Item> findByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);

    // WHERE owner_id = ? AND name = ? : au plus une ligne, grâce à UNIQUE (owner_id, name)
    Optional<Item> findByOwnerIdAndName(Long ownerId, String name);
}
