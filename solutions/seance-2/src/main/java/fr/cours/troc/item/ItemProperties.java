package fr.cours.troc.item;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// TP 3 : réglages troc.item.* de application.properties (max-per-user → maxPerUser).
// Variable d'environnement équivalente : TROC_ITEM_MAXPERUSER.
@ConfigurationProperties("troc.item")
public record ItemProperties(@DefaultValue("20") int maxPerUser) {
}
