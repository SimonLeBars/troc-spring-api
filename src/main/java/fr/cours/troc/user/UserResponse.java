package fr.cours.troc.user;

// Ce que l'API renvoie d'un utilisateur : jamais le haché du mot de passe.
public record UserResponse(Long id, String name, String username) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername());
    }
}
