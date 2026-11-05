package com.paymentsapp.history.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Настройки history.* (application.yml)
@ConfigurationProperties("history")
public record HistoryProperties(
        // Длина пометки операции — по колонке operations.details
        int detailsMax,
        Page page
) {

    // Страница ленты: сколько по умолчанию и не больше скольких
    public record Page(int defaultSize, int maxSize) {
    }
}
