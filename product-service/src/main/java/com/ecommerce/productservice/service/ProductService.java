// Package declaration - this class belongs to the service package
package com.ecommerce.productservice.service;

// Import required Spring annotations
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import Lombok annotation
import lombok.RequiredArgsConstructor;

// Import entities, DTOs, repositories, and exceptions
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.exception.DuplicateResourceException;

// Import Java collections
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Service
 *
 * Business logic layer for Product operations
 * Handles CRUD operations, validations, and DTO ↔ Entity conversions
 */

// @Service marks this as a Spring service component
@Service

// @RequiredArgsConstructor generates constructor for dependency injection
@RequiredArgsConstructor

// @Transactional ensures all operations are atomic (all succeed or all fail)
@Transactional
public class ProductService {

    // Dependency: ProductRepository for product database operations
    private final ProductRepository productRepository;

    // Dependency: CategoryRepository to validate category existence
    private final CategoryRepository categoryRepository;

    /**
     * Create a new product
     *
     * Steps:
     * 1. Validate category exists
     * 2. Check for duplicate SKU
     * 3. Create product entity
     * 4. Save to database
     * 5. Return response DTO
     *
     * @param request ProductRequest with product details
     * @return ProductResponse with created product including ID
     * @throws ResourceNotFoundException if category doesn't exist
     * @throws DuplicateResourceException if SKU already exists
     */
    public ProductResponse createProduct(ProductRequest request) {
        // Step 1: Validate category exists
        // findById returns Optional<Category>
        // orElseThrow() throws exception if category not found
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()
                ));

        // Step 2: Check for duplicate SKU
        // SKU must be unique across all products
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException(
                    "Product already exists with SKU: " + request.getSku()
            );
        }

        // Step 3: Create product entity from request
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);  // Set the Category entity, not just ID
        // Set active status, default to true if not provided
        product.setActive(request.getActive() != null ? request.getActive() : true);
        // Note: id, createdAt, updatedAt are set automatically

        // Step 4: Save to database
        Product savedProduct = productRepository.save(product);

        // Step 5: Convert to response DTO and return
        return convertToResponse(savedProduct);
    }

    /**
     * Get product by ID
     *
     * @param id Product ID
     * @return ProductResponse with product details
     * @throws ResourceNotFoundException if product not found
     */
    public ProductResponse getProductById(Long id) {
        // Find product or throw exception
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        return convertToResponse(product);
    }

    /**
     * Get product by SKU
     *
     * @param sku Product SKU
     * @return ProductResponse with product details
     * @throws ResourceNotFoundException if product not found
     */
    public ProductResponse getProductBySku(String sku) {
        // Find product by SKU or throw exception
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with SKU: " + sku
                ));

        return convertToResponse(product);
    }

    /**
     * Get all products
     *
     * @return List of all products
     */
    public List<ProductResponse> getAllProducts() {
        // Retrieve all products from database
        List<Product> products = productRepository.findAll();

        // Convert each product entity to response DTO
        // Stream API: products → map to responses → collect to list
        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active products (available for sale)
     *
     * @return List of active products
     */
    public List<ProductResponse> getActiveProducts() {
        // Only retrieve products where active = true
        List<Product> products = productRepository.findByActiveTrue();

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get products by category
     *
     * @param categoryId Category ID to filter by
     * @return List of products in the category
     * @throws ResourceNotFoundException if category doesn't exist
     */
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        // Validate category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(
                    "Category not found with id: " + categoryId
            );
        }

        // Get all products in this category
        List<Product> products = productRepository.findByCategoryId(categoryId);

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active products by category
     *
     * @param categoryId Category ID to filter by
     * @return List of active products in the category
     */
    public List<ProductResponse> getActiveProductsByCategory(Long categoryId) {
        // Get only active products in the category
        List<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId);

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search products by keyword (name or description)
     *
     * @param keyword Search term
     * @return List of matching products
     */
    public List<ProductResponse> searchProducts(String keyword) {
        // Search in name and description fields (case-insensitive)
        // Only returns active products
        List<Product> products = productRepository.searchProducts(keyword);

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filter products by price range
     *
     * @param minPrice Minimum price (nullable)
     * @param maxPrice Maximum price (nullable)
     * @return List of products within price range
     */
    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products;

        // Handle different filter scenarios
        if (minPrice != null && maxPrice != null) {
            // Both min and max specified
            products = productRepository.findByPriceBetween(minPrice, maxPrice);
        } else if (minPrice != null) {
            // Only min specified
            products = productRepository.findByPriceGreaterThanEqual(minPrice);
        } else if (maxPrice != null) {
            // Only max specified
            products = productRepository.findByPriceLessThanEqual(maxPrice);
        } else {
            // Neither specified, return all products
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get products with low stock (quantity > 0 and <= threshold)
     *
     * @param threshold Stock quantity threshold (default: 10)
     * @return List of products with low stock
     */
    public List<ProductResponse> getLowStockProducts(Integer threshold) {
        // If threshold not provided, default to 10
        int stockThreshold = threshold != null ? threshold : 10;

        // Find all products where stock > 0 but <= threshold
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() > 0 && p.getStockQuantity() <= stockThreshold)
                .collect(Collectors.toList());

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get out of stock products
     *
     * @return List of products with zero stock
     */
    public List<ProductResponse> getOutOfStockProducts() {
        // Find products with stockQuantity = 0
        List<Product> products = productRepository.findByStockQuantity(0);

        return products.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing product
     *
     * @param id Product ID to update
     * @param request ProductRequest with updated data
     * @return ProductResponse with updated product
     * @throws ResourceNotFoundException if product or category not found
     * @throws DuplicateResourceException if new SKU already exists
     */
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        // Find existing product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()
                ));

        // Check for duplicate SKU if SKU is being changed
        if (!product.getSku().equals(request.getSku())) {
            // SKU is being changed - check if new SKU already exists
            if (productRepository.existsBySku(request.getSku())) {
                throw new DuplicateResourceException(
                        "Product already exists with SKU: " + request.getSku()
                );
            }
        }

        // Update product fields
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setActive(request.getActive() != null ? request.getActive() : true);
        // Note: updatedAt is automatically updated by @PreUpdate callback

        // Save updated product
        Product updatedProduct = productRepository.save(product);

        return convertToResponse(updatedProduct);
    }

    /**
     * Update product stock quantity
     *
     * Useful for inventory management without updating entire product
     *
     * @param id Product ID
     * @param quantity New stock quantity
     * @return ProductResponse with updated stock
     * @throws ResourceNotFoundException if product not found
     * @throws IllegalArgumentException if quantity is negative
     */
    public ProductResponse updateStock(Long id, Integer quantity) {
        // Validate quantity is not negative
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        // Find product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        // Update only stock quantity
        product.setStockQuantity(quantity);

        // Save and return
        Product updatedProduct = productRepository.save(product);
        return convertToResponse(updatedProduct);
    }

    /**
     * Delete a product
     *
     * @param id Product ID to delete
     * @throws ResourceNotFoundException if product not found
     */
    public void deleteProduct(Long id) {
        // Check if product exists
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + id
            );
        }

        // Delete product
        productRepository.deleteById(id);
    }

    /**
     * Soft delete a product (set active = false)
     *
     * Preferred over hard delete to preserve historical data
     *
     * @param id Product ID to deactivate
     * @return ProductResponse with updated status
     * @throws ResourceNotFoundException if product not found
     */
    public ProductResponse deactivateProduct(Long id) {
        // Find product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        // Set active to false
        product.setActive(false);

        // Save and return
        Product updatedProduct = productRepository.save(product);
        return convertToResponse(updatedProduct);
    }

    /**
     * Reactivate a product (set active = true)
     *
     * @param id Product ID to reactivate
     * @return ProductResponse with updated status
     * @throws ResourceNotFoundException if product not found
     */
    public ProductResponse activateProduct(Long id) {
        // Find product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id
                ));

        // Set active to true
        product.setActive(true);

        // Save and return
        Product updatedProduct = productRepository.save(product);
        return convertToResponse(updatedProduct);
    }

    /**
     * Convert Product entity to ProductResponse DTO
     *
     * @param product Product entity
     * @return ProductResponse DTO
     */
    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .imageUrl(product.getImageUrl())
                // Include category details
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .active(product.getActive())
                // Computed field: in stock if quantity > 0
                .inStock(product.getStockQuantity() > 0)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
