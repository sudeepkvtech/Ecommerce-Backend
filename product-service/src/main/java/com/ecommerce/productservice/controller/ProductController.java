// Package declaration - this class belongs to the controller package
package com.ecommerce.productservice.controller;

// Import Spring Web annotations
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Import Jakarta validation
import jakarta.validation.Valid;

// Import Lombok annotation
import lombok.RequiredArgsConstructor;

// Import DTOs and Service
import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.service.ProductService;

// Import Java collections and BigDecimal
import java.math.BigDecimal;
import java.util.List;

/**
 * Product Controller
 *
 * REST API endpoints for Product management
 * Provides CRUD operations and various search/filter capabilities
 */

// @RestController combines @Controller + @ResponseBody
// Returns JSON automatically
@RestController

// Base URL: /api/products
// All endpoints in this controller start with this path
@RequestMapping("/api/products")

// Constructor injection for dependencies
@RequiredArgsConstructor
public class ProductController {

    // Inject ProductService to handle business logic
    private final ProductService productService;

    /**
     * Create a new product
     *
     * Endpoint: POST /api/products
     * Request Body: ProductRequest JSON
     * Response: ProductResponse with HTTP 201 Created
     *
     * Example request:
     * {
     *   "name": "Dell XPS 13",
     *   "description": "Ultra-thin laptop",
     *   "price": 1299.99,
     *   "stockQuantity": 50,
     *   "sku": "LAPTOP-DELL-XPS13",
     *   "imageUrl": "laptop.jpg",
     *   "categoryId": 1,
     *   "active": true
     * }
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        // Create product through service
        ProductResponse response = productService.createProduct(request);

        // Return HTTP 201 Created with product details
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get product by ID
     *
     * Endpoint: GET /api/products/{id}
     * Example: GET /api/products/1
     * Response: ProductResponse with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") Long id) {

        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get product by SKU
     *
     * Endpoint: GET /api/products/sku/{sku}
     * Example: GET /api/products/sku/LAPTOP-DELL-XPS13
     * Response: ProductResponse with HTTP 200
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable("sku") String sku) {

        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all products
     *
     * Endpoint: GET /api/products
     * Response: List of ProductResponse with HTTP 200
     *
     * Returns all products (including inactive)
     * For customer-facing API, use /api/products/active instead
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        List<ProductResponse> responses = productService.getAllProducts();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all active products
     *
     * Endpoint: GET /api/products/active
     * Response: List of active products with HTTP 200
     *
     * Returns only products where active = true
     * Use this endpoint for customer-facing applications
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {

        List<ProductResponse> responses = productService.getActiveProducts();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get products by category
     *
     * Endpoint: GET /api/products/category/{categoryId}
     * Example: GET /api/products/category/1
     * Response: List of products in the category
     *
     * @RequestParam allows optional query parameters
     * Example with activeOnly: GET /api/products/category/1?activeOnly=true
     *
     * @param categoryId Category ID to filter by
     * @param activeOnly If true, return only active products (default: false)
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") Boolean activeOnly) {

        List<ProductResponse> responses;

        // Check if only active products requested
        if (activeOnly != null && activeOnly) {
            // Get only active products in category
            responses = productService.getActiveProductsByCategory(categoryId);
        } else {
            // Get all products in category
            responses = productService.getProductsByCategory(categoryId);
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Search products by keyword
     *
     * Endpoint: GET /api/products/search
     * Example: GET /api/products/search?keyword=laptop
     * Response: List of matching products
     *
     * @RequestParam extracts query parameter from URL
     * - required = true: keyword must be provided
     * - If missing: returns HTTP 400 Bad Request
     *
     * Searches in both name and description fields (case-insensitive)
     * Only returns active products
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam(value = "keyword", required = true) String keyword) {

        List<ProductResponse> responses = productService.searchProducts(keyword);
        return ResponseEntity.ok(responses);
    }

    /**
     * Filter products by price range
     *
     * Endpoint: GET /api/products/filter/price
     * Examples:
     * - GET /api/products/filter/price?minPrice=100&maxPrice=500
     * - GET /api/products/filter/price?minPrice=100
     * - GET /api/products/filter/price?maxPrice=500
     *
     * Both parameters are optional
     * - If both provided: returns products between min and max
     * - If only min: returns products >= min
     * - If only max: returns products <= max
     * - If neither: returns all products
     */
    @GetMapping("/filter/price")
    public ResponseEntity<List<ProductResponse>> filterByPrice(
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice) {

        List<ProductResponse> responses = productService.getProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get products with low stock
     *
     * Endpoint: GET /api/products/low-stock
     * Example: GET /api/products/low-stock?threshold=10
     * Response: List of products with stock <= threshold and > 0
     *
     * Useful for:
     * - Inventory management
     * - Reorder alerts
     * - Dashboard widgets
     *
     * @param threshold Stock quantity threshold (default: 10)
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @RequestParam(value = "threshold", required = false, defaultValue = "10") Integer threshold) {

        List<ProductResponse> responses = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get out of stock products
     *
     * Endpoint: GET /api/products/out-of-stock
     * Response: List of products with stock = 0
     *
     * Useful for identifying products that need restocking
     */
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<ProductResponse>> getOutOfStockProducts() {

        List<ProductResponse> responses = productService.getOutOfStockProducts();
        return ResponseEntity.ok(responses);
    }

    /**
     * Update an existing product
     *
     * Endpoint: PUT /api/products/{id}
     * Example: PUT /api/products/1
     * Request Body: ProductRequest JSON
     * Response: ProductResponse with HTTP 200
     *
     * Updates all product fields
     * For partial updates, consider PATCH method
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update product stock quantity
     *
     * Endpoint: PATCH /api/products/{id}/stock
     * Example: PATCH /api/products/1/stock?quantity=100
     * Response: ProductResponse with HTTP 200
     *
     * @PatchMapping for partial update (only stock quantity)
     * Useful for inventory management without sending entire product
     *
     * @param id Product ID
     * @param quantity New stock quantity
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable("id") Long id,
            @RequestParam(value = "quantity", required = true) Integer quantity) {

        ProductResponse response = productService.updateStock(id, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a product (hard delete)
     *
     * Endpoint: DELETE /api/products/{id}
     * Example: DELETE /api/products/1
     * Response: HTTP 204 No Content
     *
     * Permanently removes product from database
     * Consider using deactivate endpoint instead for soft delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {

        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivate a product (soft delete)
     *
     * Endpoint: PATCH /api/products/{id}/deactivate
     * Example: PATCH /api/products/1/deactivate
     * Response: ProductResponse with active=false
     *
     * Sets active = false without deleting the product
     * Preferred over hard delete to preserve historical data
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable("id") Long id) {

        ProductResponse response = productService.deactivateProduct(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a product
     *
     * Endpoint: PATCH /api/products/{id}/activate
     * Example: PATCH /api/products/1/activate
     * Response: ProductResponse with active=true
     *
     * Sets active = true, making product visible to customers again
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable("id") Long id) {

        ProductResponse response = productService.activateProduct(id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // API Endpoint Summary
    // =========================================================================
    //
    // CRUD Operations:
    // ----------------
    // POST   /api/products                    - Create product
    // GET    /api/products/{id}               - Get by ID
    // GET    /api/products                    - Get all
    // PUT    /api/products/{id}               - Update product
    // DELETE /api/products/{id}               - Delete product
    //
    // Search & Filter:
    // ----------------
    // GET    /api/products/sku/{sku}          - Get by SKU
    // GET    /api/products/active             - Get active products
    // GET    /api/products/category/{id}      - Get by category
    // GET    /api/products/search?keyword=X   - Search by keyword
    // GET    /api/products/filter/price       - Filter by price range
    //
    // Inventory Management:
    // ---------------------
    // PATCH  /api/products/{id}/stock         - Update stock
    // GET    /api/products/low-stock          - Get low stock products
    // GET    /api/products/out-of-stock       - Get out of stock products
    //
    // Activation:
    // -----------
    // PATCH  /api/products/{id}/activate      - Activate product
    // PATCH  /api/products/{id}/deactivate    - Deactivate product
    //
    // =========================================================================
    // HTTP Methods Explained
    // =========================================================================
    //
    // GET:    Retrieve resources (read-only, idempotent)
    // POST:   Create new resources (not idempotent)
    // PUT:    Update entire resource (idempotent)
    // PATCH:  Partial update (idempotent)
    // DELETE: Remove resource (idempotent)
    //
    // Idempotent: Same request multiple times = same result
    // - GET /products/1: Always returns same product
    // - DELETE /products/1: First call deletes, subsequent calls = 404
    // - POST /products: Creates new product each time (different IDs)
}
