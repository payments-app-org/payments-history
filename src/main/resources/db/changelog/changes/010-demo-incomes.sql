--liquibase formatted sql

--changeset history:010-demo-incomes
-- Пять поступлений Артуру: зарплата, переводы клиентов, оплата проекта, дивиденды (балансы — в account, 010-demo-incomes)
INSERT INTO operations (id, user_id, account_id, occurred_at, direction, category, title, details, amount, fee, currency,
                        amount_rub, mcc, transaction_code)
SELECT gen_random_uuid(), '00000000-0000-0000-0000-000000000001', i.account_id::uuid, now() - i.ago::interval, 'INCOME',
       i.category, i.title, i.details, i.amount, 0, 'RUB', i.amount,
       (SELECT default_mcc FROM categories c WHERE c.code = i.category), i.code
FROM (VALUES
    ('00000000-0000-0000-0000-00000000a002', '3 days 2 hours', 'SALARY',       'АО «Альфа-Банк»',           'Заработная плата за октябрь 2026',               312847.63, '418205736194'),
    ('00000000-0000-0000-0000-00000000a001', '2 days 5 hours', 'SBP_TRANSFER', 'Кирилл Сергеевич М',        'Спасибо, братан! Сервис — огонь',                 18500.00, '527391046283'),
    ('00000000-0000-0000-0000-00000000a002', '1 day 3 hours',  'REWARD',       'ООО «Пиксель Лаб»',         'Оплата по проекту: всё чётко, работаем дальше!', 145300.00, '639102847561'),
    ('00000000-0000-0000-0000-00000000a001', '7 hours',        'SBP_TRANSFER', 'Анна Викторовна Б',         'Проект вообще чёткий, услугой довольна',          27350.00, '740218365927'),
    ('00000000-0000-0000-0000-00000000a004', '2 hours',        'REWARD',       'АО «Инвестбанк Капитал»',   'Дивиденды по инвестиционному портфелю',          500000.00, '851320947618')
) AS i (account_id, ago, category, title, details, amount, code);
