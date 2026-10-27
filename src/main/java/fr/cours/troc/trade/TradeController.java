package fr.cours.troc.trade;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public List<Trade> findAll(@RequestParam(required = false) Long userId) {
        return tradeService.findAll(userId);
    }

    @GetMapping("/{id}")
    public Trade findById(@PathVariable Long id) {
        return tradeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Trade> propose(@RequestBody ProposeTradeRequest request) {
        Trade saved = tradeService.propose(request);
        return ResponseEntity.created(URI.create("/trades/" + saved.getId())).body(saved);
    }

    // Actions métier sur un troc existant : des sous-ressources en POST.
    @PostMapping("/{id}/accept")
    public Trade accept(@PathVariable Long id, @RequestBody TradeDecisionRequest request) {
        return tradeService.accept(id, request.userId());
    }

    @PostMapping("/{id}/reject")
    public Trade reject(@PathVariable Long id, @RequestBody TradeDecisionRequest request) {
        return tradeService.reject(id, request.userId());
    }
}
