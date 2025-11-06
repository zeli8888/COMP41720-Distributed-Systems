package zeli8888.lab4.inventoryservice.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import zeli8888.lab4.grpc.InventoryServiceGrpc.InventoryServiceImplBase;
import zeli8888.lab4.grpc.InventoryServiceProto.*;
import zeli8888.lab4.inventoryservice.service.InventoryService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InventoryGrpcService extends InventoryServiceImplBase {

    private final InventoryService inventoryService;

    @Override
    public void checkAndReduceInventory(InventoryRequest request,
                                        StreamObserver<InventoryResponse> responseObserver) {
        try {
            log.info("Received CheckAndReduceInventory request - SKU: {}, Quantity: {}",
                    request.getSkuCode(), request.getQuantity());

            if (request.getSkuCode().isBlank() || request.getQuantity() <= 0) {
                String errorMsg = "Invalid request parameters: SKU code cannot be empty and quantity must be positive";
                log.warn(errorMsg);
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(errorMsg)
                        .asRuntimeException());
                return;
            }

            boolean success = inventoryService.checkAndReduceInventory(
                    request.getSkuCode(),
                    request.getQuantity()
            );

            InventoryResponse response = InventoryResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ?
                            "Inventory reduced successfully" :
                            "Insufficient inventory or product not found")
                    .build();

            log.info("CheckAndReduceInventory completed - SKU: {}, Success: {}",
                    request.getSkuCode(), success);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in CheckAndReduceInventory for SKU: {}, Quantity: {}",
                    request.getSkuCode(), request.getQuantity(), e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void restoreInventory(InventoryRequest request,
                                 StreamObserver<InventoryResponse> responseObserver) {
        try {
            log.info("Received RestoreInventory request - SKU: {}, Quantity: {}",
                    request.getSkuCode(), request.getQuantity());

            if (request.getSkuCode().isBlank() || request.getQuantity() <= 0) {
                String errorMsg = "Invalid request parameters: SKU code cannot be empty and quantity must be positive";
                log.warn(errorMsg);
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription(errorMsg)
                        .asRuntimeException());
                return;
            }

            boolean success = inventoryService.restoreInventory(
                    request.getSkuCode(),
                    request.getQuantity()
            );

            InventoryResponse response = InventoryResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ?
                            "Inventory restored successfully" :
                            "Product not found")
                    .build();

            log.info("RestoreInventory completed - SKU: {}, Success: {}",
                    request.getSkuCode(), success);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in RestoreInventory for SKU: {}, Quantity: {}",
                    request.getSkuCode(), request.getQuantity(), e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}