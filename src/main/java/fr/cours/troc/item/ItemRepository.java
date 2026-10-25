package fr.cours.troc.item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

    Item save(Item item);

    List<Item> findAll();

    Optional<Item> findById(Long id);

    List<Item> findByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);

    void deleteById(Long id);
}
