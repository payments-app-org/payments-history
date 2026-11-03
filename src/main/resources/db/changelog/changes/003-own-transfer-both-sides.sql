--liquibase formatted sql

--changeset history:003-own-transfer-both-sides
-- У перевода между своими счетами обе строки (списание и зачисление) — у одного пользователя,
-- поэтому дубль из Kafka отсекаем по (платёж, пользователь, направление)
DROP INDEX operations_payment_user_uq;
CREATE UNIQUE INDEX operations_payment_user_direction_uq ON operations (payment_id, user_id, direction) WHERE payment_id IS NOT NULL;
