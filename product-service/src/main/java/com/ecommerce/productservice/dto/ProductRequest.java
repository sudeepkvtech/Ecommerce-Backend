// Package declaration - this class belongs to the dto package
package com.ecommerce.productservice.dto;

// Import validation annotations from Jakarta Bean Validation API
import jakarta.validation.constraints.*;
// Import Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Import BigDecimal for precise price handling
import java.math.BigDecimal;

/**
 * Product Request DTO (Data Transfer Object)
 *
 * This DTO is used when clients want to:
 * - Create a new product (POST /api/products)
 * - Update an existing product (PUT /api/products/{id})
 *
 * What data flows through this DTO?
 * ----------------------------------
 * Client → HTTP Request → JSON → ProductRequest → Service Layer → Product Entity → Database
 *
 * Example JSON that maps to this DTO:
 * ------------------------------------
 * {
 *   "name": "Dell XPS 13 Laptop",
 *   "description": "Ultra-thin laptop with 13-inch display",
 *   "price": 1299.99,
 *   "stockQuantity": 50,
 *   "sku": "LAPTOP-DELL-XPS13",
 *   "imageUrl": "https://cdn.example.com/products/dell-xps-13.jpg",
 *   "categoryId": 1,
 *   "active": true
 * }
 *
 * Why separate Request and Entity?
 * ---------------------------------
 * 1. Client shouldn't set: id, createdAt, updatedAt (server manages these)
 * 2. Validation at API boundary (fail fast if data is invalid)
 * 3. Category is sent as ID (categoryId), not full object
 * 4. Prevents over-posting attacks (client can't set restricted fields)
 */

// @Data generates getters, setters, toString, equals, hashCode
@Data

// @NoArgsConstructor generates a no-argument constructor
// Required by Jackson for JSON deserialization
@NoArgsConstructor

// @AllArgsConstructor generates a constructor with all fields
// Useful for testing and object initialization
@AllArgsConstructor
public class ProductRequest {

    /**
     * Product Name
     *
     * Validation:
     * -----------
     * @NotBlank ensures:
     * - Not null
     * - Not empty string ("")
     * - Not only whitespace ("   ")
     *
     * Why @NotBlank for name?
     * - Product must have a name for display and search
     * - Empty names break user experience
     * - Search functionality depends on this field
     *
     * Example valid values: "Dell XPS 13", "iPhone 15 Pro", "Gaming Mouse"
     * Example invalid values: null, "", "   "
     */
    @NotBlank(message = "Product name is required")

    /**
     * @Size restricts string length
     * - min = 2: Name must be at least 2 characters
     * - max = 200: Name cannot exceed 200 characters
     *
     * Why these limits?
     * - min = 2: Ensures meaningful names (prevents "A", "X")
     * - max = 200: Matches database column size
     *
     * Examples:
     * Valid: "Laptop" (6 chars), "Dell XPS 13 Ultra Thin Laptop" (30 chars)
     * Invalid: "A" (too short), "Very long product name..." (>200 chars)
     */
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    /**
     * Product Description (Optional)
     *
     * Validation:
     * -----------
     * @Size with max only:
     * - Field is optional (can be null)
     * - If provided, must not exceed 5000 characters
     *
     * Why 5000 characters?
     * - Enough for detailed product descriptions
     * - Includes specifications, features, warranty info
     * - Not too large to cause performance issues
     *
     * Description can include:
     * - Product features
     * - Technical specifications
     * - Package contents
     * - Warranty information
     * - Usage instructions
     *
     * Examples:
     * Valid: null, "", "High-performance laptop with Intel Core i7..."
     * Invalid: "Very long description..." (>5000 chars)
     */
    @Size(max = 5000, message = "Product description cannot exceed 5000 characters")
    private String description;

    /**
     * Product Price
     *
     * Validation:
     * -----------
     * @NotNull ensures price is provided
     * Why @NotNull instead of @NotBlank?
     * - @NotBlank is for strings
     * - @NotNull is for objects (including BigDecimal)
     *
     * @DecimalMin ensures price is not negative
     * - value = "0.01": Minimum price is $0.01 (one cent)
     * - inclusive = true: 0.01 is allowed
     * - Why not allow 0? Free products should be handled separately
     *
     * @Digits restricts decimal precision
     * - integer = 10: Up to 10 digits before decimal (9,999,999,999.99)
     * - fraction = 2: Exactly 2 digits after decimal (cents)
     *
     * Why BigDecimal instead of double?
     * ----------------------------------
     * double has precision errors:
     * - double price = 0.1 + 0.2; // Result: 0.30000000000000004 (WRONG!)
     * - BigDecimal price = new BigDecimal("0.1").add(new BigDecimal("0.2")); // Result: 0.3 (CORRECT!)
     *
     * For money calculations, precision is CRITICAL
     * - No rounding errors
     * - Exact decimal arithmetic
     * - Financial integrity
     *
     * Examples:
     * Valid: 0.01, 99.99, 1299.99, 9999999999.99
     * Invalid: null, 0.00, -10.50, 99.999 (too many decimals)
     */
    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Product price must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Product price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    /**
     * Stock Quantity
     *
     * Validation:
     * -----------
     * @NotNull ensures stock quantity is always specified
     * Why required?
     * - Inventory management depends on this
     * - Order fulfillment needs to check stock
     * - Cannot sell products without knowing inventory
     *
     * @Min ensures quantity is not negative
     * - value = 0: Minimum is 0 (out of stock)
     * - Negative stock makes no sense
     *
     * Stock Quantity Meaning:
     * -----------------------
     * - 0: Out of stock (cannot order)
     * - > 0: Available (can order up to this quantity)
     * - Should be updated when:
     *   1. Orders are placed (decrease)
     *   2. Orders are cancelled (increase)
     *   3. New stock arrives (increase)
     *   4. Products are damaged (decrease)
     *
     * Business Rules:
     * ---------------
     * - Don't allow orders if stockQuantity < requested quantity
     * - Alert admin when stockQuantity is low (e.g., < 10)
     * - Automatically mark product as unavailable when stockQuantity = 0
     *
     * Examples:
     * Valid: 0, 1, 50, 1000
     * Invalid: null, -5, -100
     */
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    /**
     * SKU (Stock Keeping Unit)
     *
     * What is SKU?
     * ------------
     * A unique code used to identify and track products in inventory
     * Like a product's "fingerprint" or "license plate"
     *
     * Examples:
     * - LAPTOP-DELL-XPS13
     * - PHONE-IPHONE-15-PRO-256GB-BLUE
     * - BOOK-ISBN-9780134685991
     *
     * Validation:
     * -----------
     * @NotBlank: SKU is required (crucial for inventory management)
     * @Size: Between 2 and 100 characters
     *
     * Why SKU is important?
     * ---------------------
     * 1. Unique identifier for inventory systems
     * 2. Used in warehouses to locate products
     * 3. Appears on invoices and shipping labels
     * 4. Easier to reference than long product names
     * 5. Remains constant even if product name changes
     *
     * SKU vs ID:
     * ----------
     * - ID: Internal database identifier (auto-generated, numeric)
     * - SKU: Business identifier (manually set, alphanumeric)
     * - ID changes if you migrate databases
     * - SKU stays the same across systems
     *
     * Best Practices:
     * ---------------
     * - Use meaningful codes: LAPTOP-DELL-XPS13 (good) vs XYZ123 (bad)
     * - Include category, brand, model
     * - Avoid special characters that cause issues
     * - Keep consistent format across products
     *
     * Examples:
     * Valid: "LAPTOP-001", "PHONE-IPHONE-15-PRO", "BOOK-ISBN-123456"
     * Invalid: null, "", "A", "Very long SKU..." (>100 chars)
     */
    @NotBlank(message = "Product SKU is required")
    @Size(min = 2, max = 100, message = "Product SKU must be between 2 and 100 characters")
    private String sku;

    /**
     * Product Image URL (Optional)
     *
     * Validation:
     * -----------
     * @Size with max only: Optional field, max 500 characters if provided
     *
     * What can this be?
     * -----------------
     * 1. Full URL: "https://cdn.example.com/products/laptop.jpg"
     * 2. Relative path: "/images/products/laptop.jpg"
     * 3. Multiple images (comma-separated): "img1.jpg,img2.jpg,img3.jpg"
     * 4. Cloud storage URL: "https://s3.amazonaws.com/mybucket/products/laptop.jpg"
     *
     * In production:
     * --------------
     * - Store images in cloud storage (AWS S3, Google Cloud Storage, Azure Blob)
     * - Use CDN (Content Delivery Network) for fast global access
     * - Generate thumbnails for performance
     * - Support multiple images per product (create ProductImage entity)
     *
     * Alternative approach (better for multiple images):
     * ---------------------------------------------------
     * Instead of comma-separated string, create a separate entity:
     *
     * @Entity
     * class ProductImage {
     *     Long id;
     *     String url;
     *     Integer order; // display order
     *     Boolean isPrimary; // main image
     *     Product product; // many-to-one
     * }
     *
     * Examples:
     * Valid: null, "", "https://cdn.example.com/products/laptop.jpg"
     * Invalid: "Very long URL..." (>500 chars)
     */
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    /**
     * Category ID
     *
     * Links this product to a category (e.g., Electronics, Clothing)
     *
     * Validation:
     * -----------
     * @NotNull: Every product must belong to a category
     * Why required?
     * - Products are organized by categories
     * - Users browse products by category
     * - Searching and filtering depends on categories
     *
     * Why send ID instead of full Category object?
     * ---------------------------------------------
     * Client only needs to specify which category (by ID)
     * Server will load the full Category entity from database
     *
     * Example JSON:
     * -------------
     * Bad (too much data):
     * {
     *   "name": "Laptop",
     *   "category": {
     *     "id": 1,
     *     "name": "Electronics",
     *     "description": "..."
     *   }
     * }
     *
     * Good (just the ID):
     * {
     *   "name": "Laptop",
     *   "categoryId": 1
     * }
     *
     * What happens in Service layer:
     * -------------------------------
     * 1. Receive categoryId = 1
     * 2. Load category: Category category = categoryRepository.findById(1L)
     * 3. If not found: throw CategoryNotFoundException
     * 4. Set category: product.setCategory(category)
     * 5. Save: productRepository.save(product)
     *
     * Examples:
     * Valid: 1, 5, 100
     * Invalid: null, 0, -1
     */
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    /**
     * Product Active Status (Optional)
     *
     * Indicates if product is available for sale
     * - true: Product is active (visible to customers)
     * - false: Product is inactive (hidden from customers)
     *
     * This is NOT a required field in the request
     * If not provided, defaults to true (see Product entity)
     *
     * Why make products inactive instead of deleting?
     * ------------------------------------------------
     * Soft Delete Pattern:
     * - Preserves historical data
     * - Past orders still reference this product
     * - Can be reactivated later
     * - No broken foreign key relationships
     * - Audit trail remains intact
     *
     * When to set active = false?
     * ---------------------------
     * 1. Product discontinued (no longer manufactured)
     * 2. Seasonal products (winter clothes in summer)
     * 3. Out of stock with no restock plans
     * 4. Product recall or quality issues
     * 5. Temporary unavailability
     *
     * Database query impact:
     * ----------------------
     * Most queries should filter by active = true:
     * - List products: SELECT * FROM products WHERE active = true
     * - Search products: ... WHERE name LIKE ? AND active = true
     * - Category products: ... WHERE category_id = ? AND active = true
     *
     * Admin interface can show inactive products:
     * - SELECT * FROM products WHERE active = false
     * - Allows admin to reactivate products
     *
     * Examples:
     * Valid: true, false, null (defaults to true)
     */
    private Boolean active;

    // =========================================================================
    // Validation Flow
    // =========================================================================
    //
    // 1. Client sends POST request with JSON:
    //    POST /api/products
    //    Content-Type: application/json
    //    {
    //      "name": "Dell XPS 13",
    //      "price": 1299.99,
    //      "stockQuantity": 50,
    //      "sku": "LAPTOP-DELL-XPS13",
    //      "categoryId": 1
    //    }
    //
    // 2. Spring converts JSON to ProductRequest object (Jackson)
    //
    // 3. @Valid annotation in Controller triggers validation:
    //    @PostMapping
    //    public ResponseEntity<ProductResponse> createProduct(
    //        @Valid @RequestBody ProductRequest request) {
    //        // ...
    //    }
    //
    // 4. If validation fails:
    //    - HTTP 400 Bad Request
    //    - JSON with validation errors:
    //      {
    //        "status": 400,
    //        "errors": [
    //          {
    //            "field": "price",
    //            "message": "Product price must be at least 0.01",
    //            "rejectedValue": 0.00
    //          }
    //        ]
    //      }
    //
    // 5. If validation succeeds:
    //    - Request reaches Controller method
    //    - Controller calls Service layer
    //    - Service converts DTO to Entity:
    //      Product product = new Product();
    //      product.setName(request.getName());
    //      product.setPrice(request.getPrice());
    //      // ... set other fields
    //      Category category = categoryRepository.findById(request.getCategoryId());
    //      product.setCategory(category);
    //      productRepository.save(product);
    //
    // =========================================================================
    // Example Usage in Controller
    // =========================================================================
    //
    // @PostMapping("/api/products")
    // public ResponseEntity<ProductResponse> createProduct(
    //     @Valid @RequestBody ProductRequest request) {
    //
    //     // If code reaches here, all validation passed
    //     ProductResponse response = productService.createProduct(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }
}
