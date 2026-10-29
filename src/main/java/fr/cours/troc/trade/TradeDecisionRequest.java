package fr.cours.troc.trade;

// Corps de POST /trades/{id}/accept et /reject : l'utilisateur qui répond au troc.
public record TradeDecisionRequest(Long userId) {
}
