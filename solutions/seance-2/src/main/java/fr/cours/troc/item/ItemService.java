package fr.cours.troc.item;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.cours.troc.user.UserRepository;

@Service
public class ItemService {

	private final ItemRepository itemRepository;
	private final UserRepository userRepository;
	private final ItemProperties properties;

	public ItemService(ItemRepository itemRepository, UserRepository userRepository, ItemProperties properties) {
		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
		this.properties = properties;
	}

	public List<Item> findAll(Long ownerId) {
		if (ownerId == null) {
			return itemRepository.findAll();
		}
		return itemRepository.findByOwnerId(ownerId);
	}

	public Item findById(Long id) {
		return itemRepository.findById(id).orElseThrow(() ->
				new ResponseStatusException(HttpStatus.NOT_FOUND, "Objet " + id + " introuvable"));
	}

	public Item create(Item item) {
		checkItem(item);
		// TP 3 : 409, la requête est valide mais l'état actuel des données l'empêche
		if (itemRepository.countByOwnerId(item.getOwnerId()) >= properties.maxPerUser()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"L'utilisateur " + item.getOwnerId() + " a déjà " + properties.maxPerUser() + " objets");
		}
		item.setId(null); // l'id est attribué par le repository, jamais par le client
		return itemRepository.save(item);
	}

	public Item update(Long id, Item item) {
		findById(id); // 404 si l'objet n'existe pas
		checkItem(item);
		item.setId(id);
		return itemRepository.save(item);
	}

	public void delete(Long id) {
		findById(id);
		itemRepository.deleteById(id);
	}

	// Vérifications communes à create et update, dans l'ordre : existe (404), puis cohérent (400)
	private void checkItem(Item item) {
		// TP 2 : l'IllegalArgumentException de la séance 1 donnait un 500
		if (userRepository.findById(item.getOwnerId()).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Utilisateur " + item.getOwnerId() + " introuvable");
		}
		if (item.getQuantity() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être positive");
		}
	}
}
