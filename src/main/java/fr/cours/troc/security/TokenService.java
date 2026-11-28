package fr.cours.troc.security;

import fr.cours.troc.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

// Fabrique les jetons. Ils sont vérifiés ailleurs, à chaque requête, par le JwtDecoder de SecurityConfig.
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    public TokenResponse createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("troc-api")
                .subject(String.valueOf(user.getId())) // qui : lu par @AuthenticationPrincipal Jwt
                .claim("username", user.getUsername())  // lisible par le client, donc jamais de secret
                .issuedAt(now)
                .expiresAt(now.plus(props.ttl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build(); // sans lui, l'encodeur attend une clé RSA
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, props.ttl().toSeconds());
    }
}
