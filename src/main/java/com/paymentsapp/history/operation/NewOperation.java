package com.paymentsapp.history.operation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Операция для записи в историю
public record NewOperation(
        UUID userId,
        UUID accountId,
        OffsetDateTime occurredAt,
        Direction direction,
        String category,
        String title,
        String details,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        BigDecimal amountRub,
        BigDecimal cashback,
        LoyaltyProgram loyaltyProgram,
        UUID counterpartyUserId,
        UUID paymentId
) {
}
