package zeli8888.lab4.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import zeli8888.lab4.orderservice.model.dto.OrderDTO;
import zeli8888.lab4.orderservice.model.dto.OrderRequest;
import zeli8888.lab4.orderservice.model.entity.Order;
import zeli8888.lab4.events.OrderPlacedEvent;
import zeli8888.lab4.orderservice.repository.OrderRepository;
import zeli8888.lab4.orderservice.service.client.InventoryGrpcClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final InventoryGrpcClient inventoryGrpcClient;

    public boolean createOrder(OrderRequest orderRequest) {
        if (!inventoryGrpcClient.checkAndReduceInventory(orderRequest.skuCode(), orderRequest.quantity())) return false;
        Order order = new Order();
        try{
            order.setOrderNumber(UUID.randomUUID().toString());
            order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
            order.setSkuCode(orderRequest.skuCode());
            order.setQuantity(orderRequest.quantity());
            order.setUserId(orderRequest.userDetails().userId());
            orderRepository.save(order);
        }catch (Exception e){
            inventoryGrpcClient.restoreInventory(orderRequest.skuCode(), orderRequest.quantity());
            return false;
        }

        // Send the message to Kafka Topic
        OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent();
        orderPlacedEvent.setOrderNumber(order.getOrderNumber());
        orderPlacedEvent.setEmail(orderRequest.userDetails().email());
        orderPlacedEvent.setFirstName(orderRequest.userDetails().firstName());
        orderPlacedEvent.setLastName(orderRequest.userDetails().lastName());
        log.info("Start - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);
        kafkaTemplate.send("order-placed", orderPlacedEvent);
        log.info("End - Sending OrderPlacedEvent {} to Kafka topic order-placed", orderPlacedEvent);
        return true;
    }

    public List<OrderDTO> getAllOrdersForUser(String userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(order ->
                        new OrderDTO(
                                order.getOrderNumber(),
                                order.getSkuCode(),
                                order.getQuantity(),
                                order.getUserId(),
                                order.getPrice()))
                .toList();
    }
}
