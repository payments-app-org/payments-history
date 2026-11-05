package com.paymentsapp.history.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paymentsapp.platform.error.DomainException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CursorTest {

    @Test
    void roundTripsWithMicroseconds() {
        Cursor cursor = new Cursor(OffsetDateTime.parse("2026-11-03T10:15:30.123456Z"), UUID.randomUUID());

        Cursor decoded = Cursor.decode(cursor.encode());

        assertThat(decoded.occurredAt().toInstant()).isEqualTo(cursor.occurredAt().toInstant());
        assertThat(decoded.id()).isEqualTo(cursor.id());
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> Cursor.decode("???")).isInstanceOf(DomainException.class).hasMessage("Некорректный курсор");
    }
}
