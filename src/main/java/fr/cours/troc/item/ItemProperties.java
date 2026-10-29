package fr.cours.troc.item;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Réglages troc.item.* de application.properties (max-per-user → maxPerUser).
@ConfigurationProperties("troc.item")
public record ItemProperties(@DefaultValue("20") int maxPerUser) {
}
