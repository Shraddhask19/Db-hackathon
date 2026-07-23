-- =========================================================
-- QueryCraft Sample Project: E-Commerce Storefront Schema
-- Dialect Target: PostgreSQL / ANSI SQL
-- =========================================================

-- Customers & Accounts
CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(30),
    is_vip BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product Catalog
CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    category_id INT REFERENCES categories(category_id),
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    is_available BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Customer Orders & Line Items
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    customer_id INT REFERENCES customers(customer_id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(12,2) NOT NULL,
    order_status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, COMPLETED, CANCELLED, SHIPPED
    shipping_address TEXT NOT NULL
);

CREATE TABLE order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(order_id),
    product_id INT REFERENCES products(product_id),
    quantity INT NOT NULL,
    price_per_unit DECIMAL(10,2) NOT NULL
);

-- Financial Payments & Reviews
CREATE TABLE payments (
    payment_id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(order_id),
    payment_method VARCHAR(50) NOT NULL, -- CREDIT_CARD, PAYPAL, STRIPE
    amount_paid DECIMAL(12,2) NOT NULL,
    payment_status VARCHAR(30) DEFAULT 'SUCCESS',
    transaction_reference VARCHAR(100) UNIQUE,
    payment_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_reviews (
    review_id SERIAL PRIMARY KEY,
    product_id INT REFERENCES products(product_id),
    customer_id INT REFERENCES customers(customer_id),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    review_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
