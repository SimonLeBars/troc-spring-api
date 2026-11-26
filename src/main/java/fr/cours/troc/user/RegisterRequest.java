package fr.cours.troc.user;

// Corps de POST /users (inscription) : ni id, ni haché, ni rôle. Le client ne choisit que ça.
public record RegisterRequest(String name, String username, String password) {
}
