-- Uniformar nombres de tablas y reforzar reglas de unicidad.

ALTER TABLE category RENAME TO categories;
ALTER TABLE inventory RENAME TO inventories;

ALTER TABLE products RENAME CONSTRAINT fk_products_category TO fk_products_categories;
ALTER TABLE inventories RENAME CONSTRAINT fk_inventory_product TO fk_inventories_product;
ALTER TABLE inventories RENAME CONSTRAINT uk_inventory_product TO uk_inventories_product;

ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
ALTER TABLE categories ADD CONSTRAINT uk_categories_name UNIQUE (name);
