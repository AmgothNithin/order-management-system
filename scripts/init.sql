-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Seed products for inventory-service
INSERT INTO products (id, name, sku, description, category, price, stock_quantity, reserved_quantity, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'Apple iPhone 15', 'IPHONE-15-128', 'Apple iPhone 15 128GB', 'Electronics', 79999.00, 100, 0, NOW(), NOW()),
  (gen_random_uuid(), 'Samsung Galaxy S24', 'SAMSUNG-S24-256', 'Samsung Galaxy S24 256GB', 'Electronics', 74999.00, 80, 0, NOW(), NOW()),
  (gen_random_uuid(), 'Sony WH-1000XM5', 'SONY-WH1000XM5', 'Sony Noise Cancelling Headphones', 'Electronics', 24999.00, 150, 0, NOW(), NOW()),
  (gen_random_uuid(), 'Nike Air Max 270', 'NIKE-AM270-10', 'Nike Air Max 270 Size 10', 'Footwear', 9999.00, 200, 0, NOW(), NOW()),
  (gen_random_uuid(), 'Levi Strauss 501 Jeans', 'LEVIS-501-32', 'Levi 501 Original Jeans 32x32', 'Clothing', 4999.00, 300, 0, NOW(), NOW())
ON CONFLICT DO NOTHING;
