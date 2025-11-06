package zeli8888.lab4.orderservice.service.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import zeli8888.lab4.grpc.InventoryServiceProto.*;
import zeli8888.lab4.grpc.InventoryServiceGrpc;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryGrpcClient {

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    public boolean checkAndReduceInventory(String skuCode, int quantity) {
        try {
            log.debug("Calling CheckAndReduceInventory - SKU: {}, Quantity: {}", skuCode, quantity);

            if (skuCode == null || skuCode.trim().isEmpty()) {
                log.warn("Invalid SKU code: {}", skuCode);
                return false;
            }
            if (quantity <= 0) {
                log.warn("Invalid quantity: {}", quantity);
                return false;
            }

            InventoryRequest request = InventoryRequest.newBuilder()
                    .setSkuCode(skuCode)
                    .setQuantity(quantity)
                    .build();

            InventoryResponse response = inventoryStub.checkAndReduceInventory(request);

            log.info("CheckAndReduceInventory result - SKU: {}, Success: {}, Message: {}",
                    skuCode, response.getSuccess(), response.getMessage());

            return response.getSuccess();

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                log.error("Inventory service is unavailable - SKU: {}", skuCode, e);
            } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                log.error("Inventory service call timeout - SKU: {}", skuCode, e);
            } else {
                log.error("gRPC call failed for CheckAndReduceInventory - SKU: {}, Status: {}",
                        skuCode, e.getStatus(), e);
            }
            return false;

        } catch (Exception e) {
            log.error("Unexpected error in CheckAndReduceInventory - SKU: {}", skuCode, e);
            return false;
        }
    }

    public boolean restoreInventory(String skuCode, int quantity) {
        try {
            log.debug("Calling RestoreInventory - SKU: {}, Quantity: {}", skuCode, quantity);

            if (skuCode == null || skuCode.trim().isEmpty()) {
                log.warn("Invalid SKU code: {}", skuCode);
                return false;
            }
            if (quantity <= 0) {
                log.warn("Invalid quantity: {}", quantity);
                return false;
            }

            InventoryRequest request = InventoryRequest.newBuilder()
                    .setSkuCode(skuCode)
                    .setQuantity(quantity)
                    .build();

            InventoryResponse response = inventoryStub.restoreInventory(request);

            log.info("RestoreInventory result - SKU: {}, Success: {}, Message: {}",
                    skuCode, response.getSuccess(), response.getMessage());

            return response.getSuccess();

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                log.error("Inventory service is unavailable for restore - SKU: {}", skuCode, e);
            } else if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("SKU not found for restore - SKU: {}", skuCode);
            } else {
                log.error("gRPC call failed for RestoreInventory - SKU: {}, Status: {}",
                        skuCode, e.getStatus(), e);
            }
            return false;

        } catch (Exception e) {
            log.error("Unexpected error in RestoreInventory - SKU: {}", skuCode, e);
            return false;
        }
    }
}