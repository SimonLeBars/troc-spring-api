package fr.cours.troc.trade;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.cours.troc.item.Item;
import fr.cours.troc.item.ItemService;
import fr.cours.troc.user.UserService;

@Service
public class TradeService {

	private final TradeRepository tradeRepository;
	private final UserService userService;
	private final ItemService itemService;
	private final TradeProperties properties;

	// Dépend des services (et non de leurs repositories) : on profite de leurs règles, en particulier du 404.
	public TradeService(TradeRepository tradeRepository, UserService userService, ItemService itemService,
			TradeProperties properties) {
		this.tradeRepository = tradeRepository;
		this.userService = userService;
		this.itemService = itemService;
		this.properties = properties;
	}

	public List<Trade> findAll(Long userId) {
		if (userId == null) {
			return tradeRepository.findAll();
		}
		return tradeRepository.findByProposerIdOrRecipientId(userId, userId);
	}

	public Trade findById(Long id) {
		return tradeRepository.findById(id).orElseThrow(() ->
				new ResponseStatusException(HttpStatus.NOT_FOUND, "Troc " + id + " introuvable"));
	}

	public Trade propose(ProposeTradeRequest request) {
		// Règles 1 et 2 (404) : findById lève l'erreur si l'utilisateur ou l'objet n'existe pas
		userService.findById(request.proposerId());
		userService.findById(request.recipientId());
		Item offered = itemService.findById(request.offeredItemId());
		Item requested = itemService.findById(request.requestedItemId());

		// Règles 3 à 5 (400) : la requête est incohérente en elle-même
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

		// Règle 6 : 409. La requête est valide en elle-même : la même proposition serait acceptée
		// avec des stocks plus importants. C'est l'état actuel des données qui l'empêche.
		checkStock(offered, request.offeredQuantity());
		checkStock(requested, request.requestedQuantity());

		// Règle 7 (409) : limite de trocs en attente, réglée par troc.trade.max-pending
		if (tradeRepository.countByProposerIdAndStatus(request.proposerId(), TradeStatus.PROPOSED)
				>= properties.maxPending()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"L'utilisateur " + request.proposerId() + " a déjà " + properties.maxPending() + " trocs en attente");
		}

		// Le service construit l'entité après les vérifications : id et statut sont décidés par le serveur.
		// Une proposition ne modifie aucun stock (l'échange arrive au TP 6).
		Trade trade = new Trade();
		trade.setStatus(TradeStatus.PROPOSED);
		trade.setProposerId(request.proposerId());
		trade.setOfferedItemId(request.offeredItemId());
		trade.setOfferedQuantity(request.offeredQuantity());
		trade.setRecipientId(request.recipientId());
		trade.setRequestedItemId(request.requestedItemId());
		trade.setRequestedQuantity(request.requestedQuantity());
		return tradeRepository.save(trade);
	}

	private void checkStock(Item item, int quantity) {
		if (quantity > item.getQuantity()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Stock insuffisant : l'objet " + item.getId() + " n'a que " + item.getQuantity() + " unités");
		}
	}
}
