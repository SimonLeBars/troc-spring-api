package fr.cours.troc.user;

import fr.cours.troc.item.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Séance 4 : premier test unitaire, sans Spring ni base. Les dépendances sont des faux (mocks).
class UserServiceTest {

    @Test
    void registerStoresHashNotPassword() {
        // given : un faux repository qui renvoie ce qu'on lui donne à enregistrer,
        // et un faux encodeur qui « hache » tomate42 en une valeur connue
        UserRepository users = mock(UserRepository.class);
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("tomate42")).thenReturn("{bcrypt}haché-de-tomate42");
        UserService service = new UserService(users, mock(ItemRepository.class), encoder);

        // when
        User created = service.register(new RegisterRequest("Alice", "alice", "tomate42"));

        // then : c'est le haché qui est enregistré, jamais le mot de passe ; l'id est laissé à la base
        assertEquals("alice", created.getUsername());
        assertEquals("{bcrypt}haché-de-tomate42", created.getPasswordHash());
        assertNull(created.getId());
    }

    @Test
    void deleteRefusedOnSomeoneElsesAccount() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(users, mock(ItemRepository.class), mock(PasswordEncoder.class));

        // Mallory (3) tente de supprimer le compte de Bob (2)
        var ex = assertThrows(ResponseStatusException.class, () -> service.delete(2L, 3L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(users, never()).deleteById(any());
    }

    @Test
    void registerRefusedWhenUsernameTaken() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByUsername("bob")).thenReturn(Optional.of(new User()));
        UserService service = new UserService(users, mock(ItemRepository.class), mock(PasswordEncoder.class));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest("Bob", "bob", "carotte42")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(users, never()).save(any());
    }
}
