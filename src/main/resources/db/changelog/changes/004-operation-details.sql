--liquibase formatted sql

--changeset history:004-operation-details
-- Детали операции для карточки и чека: MCC (код категории продавца), скидка, код транзакции
ALTER TABLE operations ADD COLUMN mcc VARCHAR(4);
ALTER TABLE operations ADD COLUMN discount NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE operations ADD COLUMN transaction_code VARCHAR(20);
ALTER TABLE categories ADD COLUMN default_mcc VARCHAR(4);

UPDATE categories SET default_mcc = v.mcc FROM (VALUES
    ('SBP_TRANSFER', '4829'), ('CARD_TRANSFER', '4829'), ('OWN_TRANSFER', '6012'), ('SALARY', '6012'),
    ('REWARD', '6012'), ('SUPERMARKETS', '5411'), ('CAFE', '5812'), ('CLOTHES', '5651'), ('MARKETPLACES', '5399'),
    ('PHARMACY', '5912'), ('MOBILE', '4814'), ('INTERNET', '4899'), ('UTILITIES', '4900'), ('TAXI', '4121'),
    ('TRANSPORT', '4111'), ('ENTERTAINMENT', '7832'), ('TRAVEL', '4722'), ('CASH', '6011'), ('OTHER', '5999')
) AS v (code, mcc) WHERE categories.code = v.code;

-- Заполняем уже сохранённые операции: MCC по категории, код транзакции — 12 случайных цифр,
-- скидка партнёра — 5% у покупок по программе «Скидки»
UPDATE operations o SET mcc = c.default_mcc FROM categories c WHERE c.code = o.category;
UPDATE operations SET transaction_code = lpad((floor(random() * 1e12))::bigint::text, 12, '0');
UPDATE operations SET discount = round(amount * 0.05, 2) WHERE loyalty_program = 'DISCOUNT';
