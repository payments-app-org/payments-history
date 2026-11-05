package com.paymentsapp.history.operation;

// Категории, в которые попадают платежи из payment (коды из таблицы categories).
// Остальные категории (кафе, супермаркеты, зарплата…) приходят из демо-данных и будущих источников.
public enum CategoryCode {
    SBP_TRANSFER,
    CARD_TRANSFER,
    OWN_TRANSFER,
    ABROAD_TRANSFER,
    WALLET_TRANSFER,
    REQUISITES,
    MOBILE,
    GOV,
    TRANSPORT,
    GAMES,
    EMONEY,
    EDUCATION,
    MARKETPLACES,
    TRAVEL,
    INSURANCE,
    SECURITY,
    DONATIONS,
    UTILITIES,
    INTERNET,
    OTHER
}
