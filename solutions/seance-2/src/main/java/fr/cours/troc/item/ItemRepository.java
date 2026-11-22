package fr.cours.troc.item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

	Item save(Item item);

	List<Item> findAll();

	Optional<Item> findById(Long id);

	// noms choisis pour que Spring Data sache écrire ces requêtes seul au TP 5
	List<Item> findByOwnerId(Long ownerId);

	long countByOwnerId(Long ownerId);

	void deleteById(Long id);
}
