package zeli8888.lab4.orderservice.model.dto;

import java.math.BigDecimal;

public record OrderDTO(String orderNumber,
                       String skuCode,
                       Integer quantity,
                       String userId,
                       BigDecimal price) {
}
