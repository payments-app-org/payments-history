--liquibase formatted sql

--changeset history:008-bill-groups
INSERT INTO categories (code, name, sort_order, default_mcc) VALUES
    ('EMONEY',    'Электронные деньги', 166, '6051'),
    ('EDUCATION', 'Образование',        167, '8299'),
    ('INSURANCE', 'Страхование',        168, '6300');
