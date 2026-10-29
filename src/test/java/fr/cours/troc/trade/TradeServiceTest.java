package fr.cours.troc.trade;

import fr.cours.troc.item.Item;
import fr.cours.troc.item.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Séance 4 : les règles d'acceptation d'un troc, testées sans Spring ni base.
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    TradeRepository trades;
    @Mock
    ItemService itemService;
    @InjectMocks
    TradeService service; // construit avec les mocks ci-dessus (UserService et TradeProperties reçoivent null)

    @Test
    void acceptRefusedWhenStockTooLow() {
        // given : Bob n'a plus qu'1 carotte, le troc en demande 4
        when(trades.findById(7L)).thenReturn(Optional.of(proposedTrade(10L, 3, 20L, 4)));
        when(itemService.findById(10L)).thenReturn(item(10L, 1L, "tomates", 10));
        when(itemService.findById(20L)).thenReturn(item(20L, 2L, "carottes", 1));

        // when / then
        var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(itemService, never()).transfer(any(), anyInt(), any()); // aucun stock touché
        verify(trades, never()).save(any());                                    // statut inchangé
    }

    @Test
    void acceptRefusedWhenNotRecipient() {
        when(trades.findById(7L)).thenReturn(Optional.of(proposedTrade(10L, 3, 20L, 4)));

        // Alice (1) tente d'accepter sa propre proposition
        var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(trades, never()).save(any());
    }

    @Test
    void acceptRefusedWhenAlreadyExecuted() {
        Trade trade = proposedTrade(10L, 3, 20L, 4);
        trade.setStatus(TradeStatus.EXECUTED);
        when(trades.findById(7L)).thenReturn(Optional.of(trade));

        var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(trades, never()).save(any());
    }

    @Test
    void acceptTransfersBothItemsAndExecutesTrade() {
        Item tomatoes = item(10L, 1L, "tomates", 10);
        Item carrots = item(20L, 2L, "carottes", 5);
        when(trades.findById(7L)).thenReturn(Optional.of(proposedTrade(10L, 3, 20L, 4)));
        when(itemService.findById(10L)).thenReturn(tomatoes);
        when(itemService.findById(20L)).thenReturn(carrots);
        when(trades.save(any())).thenAnswer(call -> call.getArgument(0));

        Trade result = service.accept(7L, 2L);

        assertEquals(TradeStatus.EXECUTED, result.getStatus());
        verify(itemService).transfer(tomatoes, 3, 2L); // 3 tomates d'Alice vers Bob
        verify(itemService).transfer(carrots, 4, 1L);  // 4 carottes de Bob vers Alice
    }

    // Un troc PROPOSED d'Alice (1) vers Bob (2).
    private static Trade proposedTrade(Long offeredItemId, int offeredQuantity,
                                       Long requestedItemId, int requestedQuantity) {
        Trade trade = new Trade();
        trade.setId(7L);
        trade.setStatus(TradeStatus.PROPOSED);
        trade.setProposerId(1L);
        trade.setOfferedItemId(offeredItemId);
        trade.setOfferedQuantity(offeredQuantity);
        trade.setRecipientId(2L);
        trade.setRequestedItemId(requestedItemId);
        trade.setRequestedQuantity(requestedQuantity);
        return trade;
    }

    private static Item item(Long id, Long ownerId, String name, int quantity) {
        Item item = new Item();
        item.setId(id);
        item.setOwnerId(ownerId);
        item.setName(name);
        item.setQuantity(quantity);
        return item;
    }
}
