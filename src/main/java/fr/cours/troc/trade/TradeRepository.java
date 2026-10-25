package fr.cours.troc.trade;

import java.util.List;
import java.util.Optional;

public interface TradeRepository {

    Trade save(Trade trade);

    List<Trade> findAll();

    Optional<Trade> findById(Long id);

    // trocs où l'utilisateur est proposeur OU destinataire
    List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId);

    long countByProposerIdAndStatus(Long proposerId, TradeStatus status);
}
