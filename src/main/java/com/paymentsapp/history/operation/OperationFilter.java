package com.paymentsapp.history.operation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Фильтры истории. Пустой список или null — фильтр не применяется. to — не включительно.
public record OperationFilter(
        UUID userId,
        UUID accountId,
        OffsetDateTime from,
        OffsetDateTime to,
        Direction direction,
        List<String> categories,
        List<LoyaltyProgram> loyalty,
        List<String> currencies,
        UUID counterpartyUserId
) {
}
