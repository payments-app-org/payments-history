package com.paymentsapp.history.operation;

import com.paymentsapp.history.error.HistoryError;
import com.paymentsapp.platform.error.DomainException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

// Курсор пагинации — позиция последней отданной операции. Стабилен, даже если сверху появились новые операции.
public record Cursor(OffsetDateTime occurredAt, UUID id) {

    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_REGEX = "\\|";

    public String encode() {
        String raw = occurredAt.toInstant() + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String value) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split(SEPARATOR_REGEX);
            return new Cursor(Instant.parse(parts[0]).atOffset(ZoneOffset.UTC), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw new DomainException(HistoryError.CURSOR_INVALID);
        }
    }
}
