-- Add optimized composite indexes to accelerate common listing queries
ALTER TABLE products
    DROP INDEX idx_products_seller_status,
    ADD INDEX idx_products_seller_status_create_time (seller_id, status, create_time),
    DROP INDEX idx_products_audit_status,
    ADD INDEX idx_products_audit_status_status_create_time (audit_status, status, create_time);

ALTER TABLE orders
    DROP INDEX idx_orders_buyer_status,
    ADD INDEX idx_orders_buyer_status_create_time (buyer_id, status, create_time),
    DROP INDEX idx_orders_seller_status,
    ADD INDEX idx_orders_seller_status_create_time (seller_id, status, create_time),
    ADD INDEX idx_orders_buyer_create_time (buyer_id, create_time),
    ADD INDEX idx_orders_seller_create_time (seller_id, create_time);

ALTER TABLE favorites
    ADD INDEX idx_favorites_user_create_time (user_id, create_time);
