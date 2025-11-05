package zeli8888.lab4.orderservice.model.dto;

import java.math.BigDecimal;

public record OrderRequest(String orderNumber, String skuCode,
                           BigDecimal price, Integer quantity, UserDetails userDetails) {

    public record UserDetails(String userId, String email, String firstName, String lastName) {
    }
}
