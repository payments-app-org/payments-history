--liquibase formatted sql

--changeset history:005-abroad-category
INSERT INTO categories (code, name, sort_order, default_mcc) VALUES ('ABROAD_TRANSFER', 'Переводы за рубеж', 25, '4829');
