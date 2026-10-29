package fr.cours.troc.trade;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface TradeRepository extends ListCrudRepository<Trade, Long> {

    // trocs où l'utilisateur est proposeur OU destinataire
    List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId);

    long countByProposerIdAndStatus(Long proposerId, TradeStatus status);
}
