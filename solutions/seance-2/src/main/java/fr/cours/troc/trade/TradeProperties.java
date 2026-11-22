package fr.cours.troc.trade;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Réglages troc.trade.* de application.properties (max-pending → maxPending).
// Variable d'environnement équivalente : TROC_TRADE_MAXPENDING.
@ConfigurationProperties("troc.trade")
public record TradeProperties(@DefaultValue("3") int maxPending) {
}
