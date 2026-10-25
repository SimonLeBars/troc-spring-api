package fr.cours.troc.trade;

import fr.cours.troc.item.Item;
import fr.cours.troc.item.ItemService;
import fr.cours.troc.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class TradeService {

    private final TradeRepository trades;
    private final UserService userService;
    private final ItemService itemService;
    private final TradeProperties props;

    public TradeService(TradeRepository trades, UserService userService, ItemService itemService,
                        TradeProperties props) {
        this.trades = trades;
        this.userService = userService;
        this.itemService = itemService;
        this.props = props;
    }

    public List<Trade> findAll(Long userId) {
        if (userId == null) {
            return trades.findAll();
        }
        return trades.findByProposerIdOrRecipientId(userId, userId);
    }

    public Trade findById(Long id) {
        return trades.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Troc " + id + " introuvable"));
    }

    public Trade propose(ProposeTradeRequest request) {
        // 404 : utilisateurs et objets existants (findById lève l'erreur)
        userService.findById(request.proposerId());
        userService.findById(request.recipientId());
        Item offered = itemService.findById(request.offeredItemId());
        Item requested = itemService.findById(request.requestedItemId());

        // 400 : la requête est incohérente en elle-même
        if (Objects.equals(request.proposerId(), request.recipientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de troquer avec soi-même");
        }
        if (!Objects.equals(offered.getOwnerId(), request.proposerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'objet " + offered.getId() + " n'appartient pas à l'utilisateur " + request.proposerId());
        }
        if (!Objects.equals(requested.getOwnerId(), request.recipientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'objet " + requested.getId() + " n'appartient pas à l'utilisateur " + request.recipientId());
        }
        if (request.offeredQuantity() <= 0 || request.requestedQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les quantités doivent être positives");
        }

        // 409 : la requête est valide, mais l'état actuel des données l'empêche
        if (request.offeredQuantity() > offered.getQuantity()
                || request.requestedQuantity() > requested.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuffisant");
        }
        if (trades.countByProposerIdAndStatus(request.proposerId(), TradeStatus.PROPOSED) >= props.maxPending()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'utilisateur " + request.proposerId() + " a déjà " + props.maxPending() + " trocs en attente");
        }

        Trade trade = new Trade();
        trade.setStatus(TradeStatus.PROPOSED);
        trade.setProposerId(request.proposerId());
        trade.setOfferedItemId(request.offeredItemId());
        trade.setOfferedQuantity(request.offeredQuantity());
        trade.setRecipientId(request.recipientId());
        trade.setRequestedItemId(request.requestedItemId());
        trade.setRequestedQuantity(request.requestedQuantity());
        return trades.save(trade);
    }
}
