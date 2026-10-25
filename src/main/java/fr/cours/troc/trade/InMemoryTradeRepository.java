package fr.cours.troc.trade;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryTradeRepository implements TradeRepository {

    private final Map<Long, Trade> trades = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public Trade save(Trade trade) {
        if (trade.getId() == null) {
            trade.setId(nextId.getAndIncrement());
        }
        trades.put(trade.getId(), trade);
        return trade;
    }

    @Override
    public List<Trade> findAll() {
        return new ArrayList<>(trades.values());
    }

    @Override
    public Optional<Trade> findById(Long id) {
        return Optional.ofNullable(trades.get(id));
    }

    @Override
    public List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId) {
        return trades.values().stream()
                .filter(trade -> Objects.equals(trade.getProposerId(), proposerId)
                        || Objects.equals(trade.getRecipientId(), recipientId))
                .toList();
    }

    @Override
    public long countByProposerIdAndStatus(Long proposerId, TradeStatus status) {
        return trades.values().stream()
                .filter(trade -> Objects.equals(trade.getProposerId(), proposerId))
                .filter(trade -> trade.getStatus() == status)
                .count();
    }
}
