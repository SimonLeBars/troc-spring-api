package fr.cours.troc.user;

import java.util.List;

public interface UserRepository {

    User save(User user);

    List<User> findAll();

    User findById(Long id);
}
