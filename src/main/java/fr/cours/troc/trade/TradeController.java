package fr.cours.troc.trade;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    // L'utilisateur connecté est lu dans le jeton (déjà vérifié par Spring Security), jamais dans le corps.
    @PostMapping
    public ResponseEntity<Trade> propose(@RequestBody ProposeTradeRequest request, @AuthenticationPrincipal Jwt jwt) {
        Trade saved = tradeService.propose(currentUserId(jwt), request);
        return ResponseEntity.created(URI.create("/trades/" + saved.getId())).body(saved);
    }

    // Actions métier sur un troc existant : des sous-ressources en POST, sans corps.
    @PostMapping("/{id}/accept")
    public Trade accept(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return tradeService.accept(id, currentUserId(jwt));
    }

    @PostMapping("/{id}/reject")
    public Trade reject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return tradeService.reject(id, currentUserId(jwt));
    }

    private static Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject()); // le subject du jeton : l'id de l'utilisateur, en texte
    }
}
