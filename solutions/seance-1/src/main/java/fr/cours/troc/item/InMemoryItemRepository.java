package fr.cours.troc.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryItemRepository implements ItemRepository {

	private final Map<Long, Item> items = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong();

	@Override
	public Item save(Item item) {
		if (item.getId() == null) {
			item.setId(sequence.incrementAndGet());
		}
		items.put(item.getId(), item);
		return item;
	}

	@Override
	public List<Item> findAll() {
		return new ArrayList<>(items.values());
	}
}
