package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.OrderDocument;
import application.adapters.out.persistence.entity.BuyerDocument;
import application.adapters.out.persistence.entity.UserDocument;
import application.adapters.out.persistence.repository.OrderMongoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import application.adapters.out.persistence.repository.UserMongoRepository;
import application.domain.enums.OrderStatus;
import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.models.Order;
import application.domain.models.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoRepositoryAdaptersTest {
    @Test
    void buyerDocumentHandlesMissingAdditionalAddresses() {
        BuyerDocument document = new BuyerDocument();

        document.setAdditionalAddresses(null);

        assertTrue(document.getAdditionalAddresses().isEmpty());
    }

    @Test
    void userAdapterMapsDomainUserToMongoDocument() {
        UserMongoRepository repository = mock(UserMongoRepository.class);
        when(repository.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MongoUserRepositoryAdapter adapter = new MongoUserRepositoryAdapter(repository);
        User user = new User(901L, "Ana", "ana@example.com", "DOC-901", Role.BUYER, UserStatus.ACTIVE);

        adapter.save(user);

        ArgumentCaptor<UserDocument> captor = ArgumentCaptor.forClass(UserDocument.class);
        verify(repository).save(captor.capture());
        assertEquals(user.getEmail(), captor.getValue().getEmail());
        when(repository.findById(901L)).thenReturn(Optional.of(captor.getValue()));
        assertEquals(user.getEmail(), adapter.findById(901L).orElseThrow().getEmail());
    }

    @Test
    void orderAdapterEmbedsItemsAndRestoresOrderState() {
        OrderMongoRepository repository = mock(OrderMongoRepository.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(repository.existsById(902L)).thenReturn(false);
        when(repository.insert(any(OrderDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MongoOrderRepositoryAdapter adapter = new MongoOrderRepositoryAdapter(repository, mongoTemplate);
        Order order = new Order(902L, 903L, OrderStatus.CART);
        order.addProduct(904L, 2, new BigDecimal("12.50"));
        order.sendToPayment();
        order.markAsPaid();

        adapter.save(order);

        ArgumentCaptor<OrderDocument> captor = ArgumentCaptor.forClass(OrderDocument.class);
        verify(repository).insert(captor.capture());
        OrderDocument saved = captor.getValue();
        assertEquals(1, saved.getItems().size());
        assertEquals(OrderStatus.PAID, saved.getStatus());
        when(repository.findById(902L)).thenReturn(Optional.of(saved));

        Order restored = adapter.findById(902L).orElseThrow();
        assertEquals(OrderStatus.PAID, restored.getStatus());
        assertEquals(1, restored.getItems().size());
        assertEquals(0, new BigDecimal("25.00").compareTo(restored.getTotal()));
    }

    @Test
    void orderAdapterRestoresCancelledOrderWithoutItems() {
        OrderMongoRepository repository = mock(OrderMongoRepository.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(repository.existsById(905L)).thenReturn(false);
        when(repository.insert(any(OrderDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MongoOrderRepositoryAdapter adapter = new MongoOrderRepositoryAdapter(repository, mongoTemplate);
        Order order = new Order(905L, 906L, OrderStatus.CART);
        order.addProduct(907L, 1, new BigDecimal("12.50"));
        order.cancel();

        adapter.save(order);

        ArgumentCaptor<OrderDocument> captor = ArgumentCaptor.forClass(OrderDocument.class);
        verify(repository).insert(captor.capture());
        when(repository.findById(905L)).thenReturn(Optional.of(captor.getValue()));

        Order restored = adapter.findById(905L).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, restored.getStatus());
        assertEquals(0, restored.getItems().size());
        assertEquals(BigDecimal.ZERO, restored.getTotal());
    }
}