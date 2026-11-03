package com.paymentsapp.history.operation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Operation(
        UUID id,
        UUID accountId,
        OffsetDateTime occurredAt,
        Direction direction,
        String category,
        String categoryName,
        String title,
        String details,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        BigDecimal cashback,
        LoyaltyProgram loyaltyProgram,
        UUID counterpartyUserId
) {
}
