package zeli8888.lab4.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zeli8888.lab4.orderservice.model.dto.OrderRequest;
import zeli8888.lab4.orderservice.service.OrderService;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody OrderRequest orderRequest) {
        boolean orderPlaced = orderService.placeOrder(orderRequest);
        if (orderPlaced) {
            return new ResponseEntity<>("Order placement completed successfully", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Order placement failed", HttpStatus.CONFLICT);
        }
    }
}
