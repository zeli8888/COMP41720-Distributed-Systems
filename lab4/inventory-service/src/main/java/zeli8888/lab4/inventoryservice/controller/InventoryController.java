package zeli8888.lab4.inventoryservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zeli8888.lab4.inventoryservice.model.dto.InventoryDTO;
import zeli8888.lab4.inventoryservice.service.InventoryService;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<Integer>> getInventory(@RequestBody List<String> skuCodes) {
        return ResponseEntity.ok(inventoryService.getInventory(skuCodes));
    }

    @PostMapping
    public ResponseEntity<String> createInventory(@RequestBody InventoryDTO inventoryDTO) {
        try {
            inventoryService.createInventory(inventoryDTO.skuCode(), inventoryDTO.quantity());
            return new ResponseEntity<>("Inventory created successfully", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Inventory creation failed", HttpStatus.CONFLICT);
        }
    }
}
