package fr.cours.troc.item;

import java.util.List;

public interface ItemRepository {

    Item save(Item item);

    List<Item> findAll();
}
