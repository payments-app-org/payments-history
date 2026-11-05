package com.paymentsapp.history.ingest;

// Тексты операций в истории, которые собирает сама history из данных события
enum OperationText {
    // Получатель или отправитель неизвестен
    TRANSFER("Перевод"),
    ROUND_UP("Округление на пользу"),
    ROUND_UP_DETAILS("Округление на пользу · %s"),
    // Между своими счетами: «На Мой кошелёк» / «С Зарплатная ·· 3752»
    TO_OWN("На %s"),
    FROM_OWN("С %s"),
    OWN_DESTINATION("свой счёт"),
    OWN_SOURCE("своего счёта"),
    // За рубеж: «1,75 USD · ARARAT BANK»
    ABROAD_DETAILS("%s %s · %s");

    private final String text;

    OperationText(String text) {
        this.text = text;
    }

    String text() {
        return text;
    }

    String format(Object... args) {
        return text.formatted(args);
    }
}
