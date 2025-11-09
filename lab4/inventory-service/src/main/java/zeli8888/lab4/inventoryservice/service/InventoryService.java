package zeli8888.lab4.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zeli8888.lab4.inventoryservice.model.Inventory;
import zeli8888.lab4.inventoryservice.repository.InventoryRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    public boolean checkAndReduceInventory(String skuCode, Integer quantity) {
        return inventoryRepository.checkAndReduceInventory(skuCode, quantity) > 0;
    }

    public boolean restoreInventory(String skuCode, Integer quantity) {
        return inventoryRepository.restoreInventory(skuCode, quantity) > 0;
    }

    public List<Integer> getInventory(List<String> skuCodes) {
        return skuCodes.stream()
                .map(skuCode -> {
                    Inventory inventory = inventoryRepository.findBySkuCode(skuCode);
                    return inventory != null ? inventory.getQuantity() : null;
                })
                .collect(Collectors.toList());
    }

    public void createInventory(String skuCode, Integer quantity) {
        if (inventoryRepository.findBySkuCode(skuCode) != null) {
            throw new RuntimeException("Inventory already exists");
        }
        inventoryRepository.save(new Inventory(null, skuCode, quantity));
    }
}
