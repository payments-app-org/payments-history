--liquibase formatted sql

--changeset history:011-demo-incomes-on-top
-- Демо-поступления — в самом начале истории: сразу после последней операции Артура.
-- Сверху вниз: зарплата, перевод клиента, оплата проекта, перевод клиента, дивиденды.
WITH latest AS (
    SELECT max(occurred_at) AS at FROM operations
    WHERE user_id = '00000000-0000-0000-0000-000000000001'
      AND transaction_code NOT IN ('418205736194', '527391046283', '639102847561', '740218365927', '851320947618')
)
UPDATE operations o SET occurred_at = latest.at + v.shift::interval
FROM latest, (VALUES
    ('418205736194', '5 minutes'),
    ('527391046283', '4 minutes'),
    ('639102847561', '3 minutes'),
    ('740218365927', '2 minutes'),
    ('851320947618', '1 minute')
) AS v (code, shift)
WHERE o.user_id = '00000000-0000-0000-0000-000000000001' AND o.transaction_code = v.code;
