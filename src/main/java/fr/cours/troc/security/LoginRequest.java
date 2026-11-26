package fr.cours.troc.security;

// Corps de POST /auth/token.
public record LoginRequest(String username, String password) {
}
