package fr.cours.troc.trade;

// Ce que le client envoie pour proposer un troc : ni id ni statut, c'est le serveur qui les décide.
// Ni proposeur : depuis le TP 5, c'est l'utilisateur du jeton.
public record ProposeTradeRequest(
        Long offeredItemId,
        int offeredQuantity,
        Long recipientId,
        Long requestedItemId,
        int requestedQuantity
) {
}
