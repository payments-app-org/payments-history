--liquibase formatted sql

--changeset history:011-roundup-operation
-- У покупки с «Округлением на пользу» две операции списания: сама покупка и округление в фонд (категория DONATIONS)
DROP INDEX operations_payment_user_direction_uq;
CREATE UNIQUE INDEX operations_payment_user_direction_uq ON operations (payment_id, user_id, direction, category)
    WHERE payment_id IS NOT NULL;
