package fr.cours.troc.trade;

// Ce que le client a le droit de choisir pour proposer un troc : ni id ni statut, c'est le serveur qui les décide.
public record ProposeTradeRequest(
		Long proposerId,
		Long offeredItemId,
		int offeredQuantity,
		Long recipientId,
		Long requestedItemId,
		int requestedQuantity) {
}
