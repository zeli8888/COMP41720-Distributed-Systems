package zeli8888.lab4.orderservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import zeli8888.lab4.orderservice.model.entity.Order;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
}
