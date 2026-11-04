--liquibase formatted sql

--changeset history:007-games
INSERT INTO categories (code, name, sort_order, default_mcc) VALUES
    ('GAMES', 'Игры и сервисы', 165, '5816');
