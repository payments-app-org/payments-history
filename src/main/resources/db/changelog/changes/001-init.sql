--liquibase formatted sql

--changeset history:001-categories
-- Справочник категорий операций
CREATE TABLE categories (
    code              VARCHAR(30) PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    sort_order        INTEGER      NOT NULL
);

INSERT INTO categories (code, name, sort_order) VALUES
    ('SBP_TRANSFER',  'Переводы через СБП',       10),
    ('CARD_TRANSFER', 'Переводы по номеру карты', 20),
    ('OWN_TRANSFER',  'Между своими счетами',     30),
    ('SALARY',        'Зарплата',                 40),
    ('REWARD',        'Вознаграждение',           50),
    ('SUPERMARKETS',  'Супермаркеты',             60),
    ('CAFE',          'Кафе и рестораны',         70),
    ('CLOTHES',       'Одежда и обувь',           80),
    ('MARKETPLACES',  'Маркетплейсы',             90),
    ('PHARMACY',      'Аптеки',                  100),
    ('MOBILE',        'Мобильная связь',         110),
    ('INTERNET',      'Интернет и ТВ',           120),
    ('UTILITIES',     'ЖКХ',                     130),
    ('TAXI',          'Такси',                   140),
    ('TRANSPORT',     'Транспорт',               150),
    ('ENTERTAINMENT', 'Развлечения',             160),
    ('TRAVEL',        'Путешествия',             170),
    ('CASH',          'Наличные',                180),
    ('OTHER',         'Прочее',                  190);

--changeset history:001-operations
-- Все операции всех пользователей. Один платёж даёт до двух строк: списание у отправителя и
-- поступление у получателя, если он наш клиент.
CREATE TABLE operations (
    id                    UUID PRIMARY KEY,
    user_id               UUID           NOT NULL,
    -- Карта или кошелёк, по которому прошла операция
    account_id            UUID,
    occurred_at           TIMESTAMPTZ    NOT NULL,
    direction             VARCHAR(10)    NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    category              VARCHAR(30)    NOT NULL REFERENCES categories (code),
    -- Спецификация: магазин, получатель/отправитель перевода, «Кешбэк», «Платье»…
    title                 VARCHAR(100)   NOT NULL,
    -- Сообщение к переводу или пометка
    details               VARCHAR(100),
    -- Сумма в валюте операции; у списаний — вместе с комиссией
    amount                NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    fee                   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    currency              VARCHAR(3)     NOT NULL CHECK (currency IN ('RUB', 'USD', 'EUR')),
    -- Сумма в рублях для итогов по категориям
    amount_rub            NUMERIC(19, 2) NOT NULL,
    cashback              NUMERIC(19, 2),
    loyalty_program       VARCHAR(20)    CHECK (loyalty_program IN ('CASHBACK', 'PREMIUM', 'DISCOUNT')),
    -- Вторая сторона перевода, если это наш клиент
    counterparty_user_id  UUID,
    payment_id            UUID,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Лента истории и пагинация по курсору (occurred_at, id)
CREATE INDEX operations_feed_idx ON operations (user_id, occurred_at DESC, id DESC);
CREATE INDEX operations_category_idx ON operations (user_id, category, occurred_at DESC);
-- Повторно прочитанное из Kafka событие не создаст дубль
CREATE UNIQUE INDEX operations_payment_user_uq ON operations (payment_id, user_id) WHERE payment_id IS NOT NULL;
