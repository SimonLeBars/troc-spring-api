package fr.cours.troc.item;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cours.troc.user.UserRepository;

@Service
public class ItemService {

	private final ItemRepository itemRepository;
	private final UserRepository userRepository;

	public ItemService(ItemRepository itemRepository, UserRepository userRepository) {
		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
	}

	public Item create(Item item) {
		if (userRepository.findById(item.getOwnerId()) == null) {
			throw new IllegalArgumentException("Propriétaire inconnu : " + item.getOwnerId());
		}
		return itemRepository.save(item);
	}

	public List<Item> findAll() {
		return itemRepository.findAll();
	}
}
