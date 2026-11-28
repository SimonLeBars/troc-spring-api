package fr.cours.troc.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Les réglages troc.jwt.* : la clé de signature (variable d'environnement TROC_JWT_SECRET) et la durée de vie.
@ConfigurationProperties("troc.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
