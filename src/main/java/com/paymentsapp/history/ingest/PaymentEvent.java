package com.paymentsapp.history.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Итог платежа из топика payment.events — поля, нужные истории. Тип платежа и группа поставщика — строками:
// новый тип в payment не должен ронять чтение событий (такой платёж попадёт в «Другое»).
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentEvent(
        String eventType, OffsetDateTime occurredAt, UUID paymentId, UUID userId, String type,
        UUID sourceAccountId, String senderName,
        UUID recipientUserId, UUID recipientAccountId, String recipientName, String recipientPhone, String recipientCardMask,
        String message, Boolean destinationExternal, String purpose,
        BigDecimal roundUpAmount, String roundUpFundName,
        String providerGroup,
        String receiveCurrency, BigDecimal receiveAmount, String transferMethodName,
        OffsetDateTime createdAt, BigDecimal amount, BigDecimal commission, BigDecimal total, String currency
) {

    private static final String SUCCEEDED = "PaymentSucceeded";
    private static final String DEFAULT_CURRENCY = "RUB";

    public boolean succeeded() {
        return SUCCEEDED.equals(eventType);
    }

    // Время платежа; у старых событий — только время публикации
    public OffsetDateTime at() {
        return createdAt != null ? createdAt : occurredAt;
    }

    public String currencyOrDefault() {
        return currency != null ? currency : DEFAULT_CURRENCY;
    }

    // Зачисление на свою карту другого банка: поступления в нашей истории нет. Поля может не быть — значит, нет
    public boolean toExternalAccount() {
        return Boolean.TRUE.equals(destinationExternal);
    }

    public BigDecimal roundUp() {
        return roundUpAmount != null ? roundUpAmount : BigDecimal.ZERO;
    }
}
