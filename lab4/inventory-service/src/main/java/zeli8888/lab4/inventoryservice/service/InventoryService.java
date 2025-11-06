package zeli8888.lab4.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zeli8888.lab4.inventoryservice.model.Inventory;
import zeli8888.lab4.inventoryservice.repository.InventoryRepository;

import java.util.Collection;
import java.util.List;
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

    public List<Integer> getInventory(Collection<String> skuCodes) {
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(Inventory::getQuantity)
                .collect(Collectors.toList());
    }

    public void createInventory(String skuCode, Integer quantity) {
        inventoryRepository.save(new Inventory(null, skuCode, quantity));
    }
}
