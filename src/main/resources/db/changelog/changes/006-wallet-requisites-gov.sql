--liquibase formatted sql

--changeset history:006-wallet-requisites-gov
INSERT INTO categories (code, name, sort_order, default_mcc) VALUES
    ('WALLET_TRANSFER', 'Переводы на кошелёк',       26, '4829'),
    ('REQUISITES',      'Платежи по реквизитам',     27, '4829'),
    ('GOV',             'Госуслуги, штрафы, налоги', 135, '9399');
