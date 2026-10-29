package fr.cours.troc.trade;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Réglages troc.trade.* de application.properties (max-pending → maxPending).
@ConfigurationProperties("troc.trade")
public record TradeProperties(@DefaultValue("3") int maxPending) {
}
