package fr.cours.troc.user;

import fr.cours.troc.item.ItemRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Séance 4 : premier test unitaire, sans Spring ni base. Les repositories sont des faux (mocks).
class UserServiceTest {

    @Test
    void createIgnoresIdSentByClient() {
        // given : un faux repository qui renvoie ce qu'on lui donne à enregistrer
        UserRepository users = mock(UserRepository.class);
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        UserService service = new UserService(users, mock(ItemRepository.class));

        User user = new User();
        user.setId(42L);
        user.setName("Alice");

        // when
        User created = service.create(user);

        // then : le nom est conservé, l'id du client est effacé (c'est la base qui l'attribue)
        assertEquals("Alice", created.getName());
        assertNull(created.getId());
    }
}
