package fr.cours.troc.user;

// Corps de PUT /users/{id} : seul le nom affiché se modifie. Recevoir l'entité User effacerait le haché.
public record UpdateUserRequest(String name) {
}
