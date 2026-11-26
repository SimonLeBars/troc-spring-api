package fr.cours.troc.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

// L'entité n'est jamais renvoyée telle quelle au client : elle contient le haché du mot de passe
// (voir UserResponse).
@Table("users")
public class User {

    @Id
    private Long id; // null → INSERT, sinon UPDATE
    private String name;
    private String username;     // identifiant de connexion, unique
    private String passwordHash; // colonne password_hash : le haché BCrypt, jamais le mot de passe

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
