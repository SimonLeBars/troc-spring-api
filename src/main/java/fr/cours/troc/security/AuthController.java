package fr.cours.troc.security;

import fr.cours.troc.user.User;
import fr.cours.troc.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, UserRepository users,
                          TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.tokenService = tokenService;
    }

    // Connexion : route ouverte (voir SecurityConfig).
    @PostMapping("/token")
    public TokenResponse token(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException e) {
            // Même message que le compte existe ou non : ne pas aider à deviner les identifiants.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect");
        }
        User user = users.findByUsername(request.username()).orElseThrow(); // existe : l'authentification a réussi
        return tokenService.createToken(user);
    }
}
