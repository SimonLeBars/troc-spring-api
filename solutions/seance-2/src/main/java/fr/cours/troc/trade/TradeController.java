package fr.cours.troc.trade;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
public class TradeController {

	private final TradeService tradeService;

	public TradeController(TradeService tradeService) {
		this.tradeService = tradeService;
	}

	@GetMapping
	public List<Trade> list(@RequestParam(required = false) Long userId) {
		return tradeService.findAll(userId);
	}

	@GetMapping("/{id}")
	public Trade findById(@PathVariable Long id) {
		return tradeService.findById(id);
	}

	// Proposer un troc, c'est créer une ressource troc : POST /trades, sans verbe dans l'URL.
	// Le corps est un ProposeTradeRequest, jamais un Trade : le client ne choisit ni l'id ni le statut.
	@PostMapping
	public ResponseEntity<Trade> propose(@RequestBody ProposeTradeRequest request) {
		Trade saved = tradeService.propose(request);
		return ResponseEntity.created(URI.create("/trades/" + saved.getId())).body(saved);
	}
}
