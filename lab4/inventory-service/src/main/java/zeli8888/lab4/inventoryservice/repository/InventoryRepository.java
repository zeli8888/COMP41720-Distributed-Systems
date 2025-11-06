package zeli8888.lab4.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zeli8888.lab4.inventoryservice.model.Inventory;

import java.util.Collection;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity " +
            "WHERE i.skuCode = :skuCode AND i.quantity >= :quantity")
    int checkAndReduceInventory(@Param("skuCode") String skuCode,
                                @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity " +
            "WHERE i.skuCode = :skuCode")
    int restoreInventory(@Param("skuCode") String skuCode,
                         @Param("quantity") Integer quantity);

    List<Inventory> findBySkuCodeIn(Collection<String> skuCodes);
}
