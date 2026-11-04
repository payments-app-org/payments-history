package com.paymentsapp.history.ingest;

import com.paymentsapp.history.operation.Direction;
import com.paymentsapp.history.operation.NewOperation;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

// Успешный платёж → операции в истории: списание у отправителя и, если получатель наш клиент, поступление у него.
// Между своими счетами обе операции у одного пользователя: «На Мой кошелёк» и «С Зарплатная ·· 3752».
public final class PaymentEventMapper {

    private PaymentEventMapper() {
    }

    public static List<NewOperation> toOperations(JsonNode e) {
        if (!"PaymentSucceeded".equals(e.path("eventType").asString(""))) {
            return List.of();
        }
        UUID paymentId = uuid(e, "paymentId");
        String category = switch (e.path("type").asString("")) {
            case "CARD2CARD" -> "CARD_TRANSFER";
            case "OWN_TRANSFER" -> "OWN_TRANSFER";
            case "ABROAD" -> "ABROAD_TRANSFER";
            case "WALLET_TRANSFER" -> "WALLET_TRANSFER";
            case "REQUISITES", "REQUISITES_OUR" -> "REQUISITES";
            // Оплата поставщику: категория — по группе поставщика
            case "BILL" -> switch (e.path("providerGroup").asString("")) {
                case "MOBILE", "FOREIGN_MOBILE" -> "MOBILE";
                case "GOV" -> "GOV";
                case "TRANSPORT" -> "TRANSPORT";
                case "GAMES" -> "GAMES";
                case "EMONEY" -> "EMONEY";
                case "EDUCATION" -> "EDUCATION";
                case "SHOPS" -> "MARKETPLACES";
                case "TRAVEL" -> "TRAVEL";
                case "INSURANCE" -> "INSURANCE";
                case "SECURITY" -> "SECURITY";
                case "DONATIONS" -> "DONATIONS";
                case "UTILITIES" -> "UTILITIES";
                case "INTERNET" -> "INTERNET";
                default -> "OTHER";
            };
            default -> "SBP_TRANSFER";
        };
        String currency = e.path("currency").asString("RUB");
        OffsetDateTime at = OffsetDateTime.parse(e.path(e.hasNonNull("createdAt") ? "createdAt" : "occurredAt").asString());
        BigDecimal amount = e.path("amount").decimalValue();
        BigDecimal fee = e.path("commission").decimalValue();
        BigDecimal total = e.path("total").decimalValue();
        String message = text(e, "message");
        // Юрлицу и в бюджет — в пометке назначение платежа
        String purpose = text(e, "purpose");
        if (message == null && purpose != null) {
            message = purpose.length() > 100 ? purpose.substring(0, 97) + "…" : purpose;
        }
        // За рубеж: в пометке — сколько и как получит получатель («1,75 USD · ARARAT BANK»)
        if ("ABROAD_TRANSFER".equals(category) && e.hasNonNull("receiveAmount")) {
            message = e.path("receiveAmount").decimalValue().stripTrailingZeros().toPlainString().replace('.', ',')
                    + " " + text(e, "receiveCurrency") + " · " + text(e, "transferMethodName");
        }
        UUID senderId = uuid(e, "userId");
        UUID recipientId = uuid(e, "recipientUserId");

        List<NewOperation> ops = new ArrayList<>();
        // Отправитель видит получателя: имя, если это наш клиент, иначе телефон или маску карты
        String recipientTitle = firstNonBlank(text(e, "recipientName"), text(e, "recipientPhone"),
                text(e, "recipientCardMask"), "Перевод");
        ops.add(new NewOperation(senderId, uuid(e, "sourceAccountId"), at, Direction.EXPENSE, category, recipientTitle,
                message, total, fee, currency, total, null, null, recipientId, paymentId));
        // Деньги, ушедшие на свою карту другого банка, в нашей истории не «приходят» — там только списание
        if (recipientId != null && !e.path("destinationExternal").asBoolean(false)) {
            String senderTitle = firstNonBlank(text(e, "senderName"), "Перевод");
            ops.add(new NewOperation(recipientId, uuid(e, "recipientAccountId"), at, Direction.INCOME, category,
                    senderTitle, message, amount, BigDecimal.ZERO, currency, amount, null, null, senderId, paymentId));
        }
        return ops;
    }

    private static String text(JsonNode e, String field) {
        String value = e.path(field).asString(null);
        return value == null || value.isBlank() ? null : value;
    }

    private static UUID uuid(JsonNode e, String field) {
        String value = text(e, field);
        return value == null ? null : UUID.fromString(value);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
