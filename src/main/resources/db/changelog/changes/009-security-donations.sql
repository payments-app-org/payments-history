--liquibase formatted sql

--changeset history:009-security-donations
INSERT INTO categories (code, name, sort_order, default_mcc) VALUES
    ('SECURITY',  'Охранные системы',   169, '7393'),
    ('DONATIONS', 'Благотворительность', 170, '8398');
