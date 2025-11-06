package zeli8888.lab4.inventoryservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zeli8888.lab4.inventoryservice.service.InventoryService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<Integer>> getInventory(Collection<String> skuCodes) {
        return ResponseEntity.ok(inventoryService.getInventory(skuCodes));
    }
}
