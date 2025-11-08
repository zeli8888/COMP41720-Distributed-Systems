package zeli8888.lab4.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zeli8888.lab4.orderservice.model.dto.OrderRequest;
import zeli8888.lab4.orderservice.service.OrderService;
import zeli8888.lab4.orderservice.model.dto.OrderDTO;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest orderRequest) {
        boolean orderPlaced = orderService.createOrder(orderRequest);
        if (orderPlaced) {
            return new ResponseEntity<>("Order placement completed successfully", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Order placement failed", HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderDTO>> getAllOrdersForUser(@PathVariable("userId") String userId) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(orderService.getAllOrdersForUser(userId), HttpStatus.OK);
    }
}
