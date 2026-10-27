package fr.cours.troc.trade;

// Stocké en texte dans la colonne status (VARCHAR), sans configuration.
public enum TradeStatus {
    PROPOSED, // accord du proposeur (séance 2)
    EXECUTED, // accord du destinataire : les stocks ont été échangés
    REJECTED  // refus du destinataire : les stocks sont intacts
}
