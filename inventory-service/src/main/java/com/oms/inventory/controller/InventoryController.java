package com.oms.inventory.controller;

import com.oms.common.dto.ApiResponse;
import com.oms.inventory.entity.Product;
import com.oms.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllProducts()));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProduct(productId)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", inventoryService.createProduct(product)));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateStock(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            Map<String, Integer> productQuantities = items.stream()
                    .collect(Collectors.toMap(
                            i -> i.get("productId").toString(),
                            i -> Integer.parseInt(i.get("quantity").toString())
                    ));
            inventoryService.validateStock(productQuantities);
            return ResponseEntity.ok(ApiResponse.success("Stock validated", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
