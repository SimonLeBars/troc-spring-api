package fr.cours.troc.security;

// Réponse de POST /auth/token : le jeton à envoyer ensuite en "Authorization: Bearer", et sa durée de vie.
public record TokenResponse(String token, long expiresIn) {
}
