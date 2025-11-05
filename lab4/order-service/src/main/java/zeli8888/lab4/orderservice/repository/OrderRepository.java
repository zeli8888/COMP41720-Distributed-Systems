package zeli8888.lab4.orderservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import zeli8888.lab4.orderservice.model.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
}
