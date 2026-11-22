package fr.cours.troc.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

	@Override
	public Optional<Item> findById(Long id) {
		if (id == null) { // id absent du JSON : ConcurrentHashMap.get(null) lèverait une NullPointerException (500)
			return Optional.empty();
		}
		return Optional.ofNullable(items.get(id));
	}

	@Override
	public List<Item> findByOwnerId(Long ownerId) {
		return items.values().stream()
				.filter(item -> Objects.equals(item.getOwnerId(), ownerId)) // jamais == entre deux Long
				.toList();
	}

	@Override
	public long countByOwnerId(Long ownerId) {
		return items.values().stream()
				.filter(item -> Objects.equals(item.getOwnerId(), ownerId))
				.count();
	}

	@Override
	public void deleteById(Long id) {
		items.remove(id);
	}
}
