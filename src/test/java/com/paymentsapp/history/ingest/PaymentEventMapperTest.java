package com.paymentsapp.history.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentsapp.history.config.HistoryProperties;
import com.paymentsapp.history.operation.Direction;
import com.paymentsapp.history.operation.NewOperation;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PaymentEventMapperTest {

    private static final int DETAILS_MAX = 100;

    private final JsonMapper json = JsonMapper.builder().build();
    private final PaymentEventMapper mapper = new PaymentEventMapper(
            new HistoryProperties(DETAILS_MAX, new HistoryProperties.Page(20, 100)));

    // Событие разбираем так же, как слушатель Kafka: проверяем и контракт с payment
    private List<NewOperation> map(String event) {
        return mapper.toOperations(json.readValue(event, PaymentEvent.class));
    }

    @Test
    void transferToOurClientGivesExpenseAndIncome() {
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"11111111-1111-1111-1111-111111111111","type":"SBP_PHONE",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "senderName":"Артур Константинович Р","recipientUserId":"00000000-0000-0000-0000-000000000002",
                 "recipientAccountId":"00000000-0000-0000-0000-00000000b002","recipientName":"Мария Сергеевна И",
                 "recipientPhone":"+79161234567","amount":100.00,"commission":1.00,"total":101.00,"currency":"RUB",
                 "message":"За обед","createdAt":"2026-11-03T10:00:00Z"}
                """);

        assertThat(ops).hasSize(2);
        NewOperation expense = ops.get(0);
        assertThat(expense.direction()).isEqualTo(Direction.EXPENSE);
        assertThat(expense.title()).isEqualTo("Мария Сергеевна И");
        assertThat(expense.amount()).isEqualByComparingTo("101.00");
        assertThat(expense.fee()).isEqualByComparingTo("1.00");
        assertThat(expense.category()).isEqualTo("SBP_TRANSFER");
        assertThat(expense.details()).isEqualTo("За обед");

        NewOperation income = ops.get(1);
        assertThat(income.direction()).isEqualTo(Direction.INCOME);
        assertThat(income.title()).isEqualTo("Артур Константинович Р");
        assertThat(income.amount()).isEqualByComparingTo("100.00");
        assertThat(income.accountId().toString()).isEqualTo("00000000-0000-0000-0000-00000000b002");
    }

    @Test
    void transferToAnotherBankGivesOnlyExpense() {
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"22222222-2222-2222-2222-222222222222","type":"CARD2CARD",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "recipientCardMask":"5469 38** **** 1234","amount":500,"commission":7.50,"total":507.50,
                 "currency":"RUB","createdAt":"2026-11-03T10:00:00Z"}
                """);

        assertThat(ops).hasSize(1);
        assertThat(ops.get(0).title()).isEqualTo("5469 38** **** 1234");
        assertThat(ops.get(0).category()).isEqualTo("CARD_TRANSFER");
    }

    @Test
    void ownTransferGivesTwoOperationsForTheSameUser() {
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"44444444-4444-4444-4444-444444444444","type":"OWN_TRANSFER",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "senderName":"Зарплатная WEEKEND ·· 3752","recipientUserId":"00000000-0000-0000-0000-000000000001",
                 "recipientAccountId":"00000000-0000-0000-0000-00000000a001","recipientName":"Мой кошелёк",
                 "amount":1000,"commission":10,"total":1010,"currency":"RUB","createdAt":"2026-11-03T10:00:00Z"}
                """);

        assertThat(ops).hasSize(2);
        assertThat(ops).allMatch(op -> op.category().equals("OWN_TRANSFER"));
        assertThat(ops.get(0).title()).isEqualTo("На Мой кошелёк");
        assertThat(ops.get(0).amount()).isEqualByComparingTo("1010");
        assertThat(ops.get(1).title()).isEqualTo("С Зарплатная WEEKEND ·· 3752");
        assertThat(ops.get(1).userId()).isEqualTo(ops.get(0).userId());
    }

    @Test
    void abroadTransferShowsReceiveAmountAndMethod() {
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"55555555-5555-5555-5555-555555555555","type":"ABROAD",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "recipientName":"ARAM PETROSYAN","amount":150,"commission":0,"total":150,"currency":"RUB",
                 "receiveCurrency":"USD","receiveAmount":1.75,"transferMethodName":"ARARAT BANK",
                 "createdAt":"2026-11-03T10:00:00Z"}
                """);

        assertThat(ops).hasSize(1);
        assertThat(ops.get(0).category()).isEqualTo("ABROAD_TRANSFER");
        assertThat(ops.get(0).title()).isEqualTo("ARAM PETROSYAN");
        assertThat(ops.get(0).details()).isEqualTo("1,75 USD · ARARAT BANK");
    }

    @Test
    void failedPaymentIsNotHistory() {
        assertThat(map("""
                {"eventType":"PaymentFailed","paymentId":"33333333-3333-3333-3333-333333333333"}
                """)).isEmpty();
    }

    @Test
    void billCategoryComesFromProviderGroupAndRoundUpIsSeparate() {
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"66666666-6666-6666-6666-666666666666","type":"BILL",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "recipientName":"Yota","providerGroup":"MOBILE","amount":300,"commission":0,"total":310,
                 "roundUpAmount":10,"roundUpFundName":"Фонд «Подари жизнь»","currency":"RUB",
                 "createdAt":"2026-11-03T10:00:00Z","someNewField":"ignored"}
                """);

        assertThat(ops).hasSize(2);
        assertThat(ops.get(0).category()).isEqualTo("MOBILE");
        assertThat(ops.get(0).amount()).isEqualByComparingTo("300");
        assertThat(ops.get(1).category()).isEqualTo("DONATIONS");
        assertThat(ops.get(1).title()).isEqualTo("Фонд «Подари жизнь»");
        assertThat(ops.get(1).details()).isEqualTo("Округление на пользу · Yota");
        assertThat(ops.get(1).amount()).isEqualByComparingTo("10");
    }

    @Test
    void unknownTypeOrGroupGoesToOther() {
        String event = """
                {"eventType":"PaymentSucceeded","paymentId":"77777777-7777-7777-7777-777777777777","type":"%s",
                 "providerGroup":"%s","userId":"00000000-0000-0000-0000-000000000001",
                 "sourceAccountId":"00000000-0000-0000-0000-00000000a002","amount":5,"commission":0,"total":5,
                 "createdAt":"2026-11-03T10:00:00Z"}
                """;

        assertThat(map(event.formatted("CRYPTO", "")).get(0).category()).isEqualTo("OTHER");
        assertThat(map(event.formatted("BILL", "MISC")).get(0).category()).isEqualTo("OTHER");
        assertThat(map(event.formatted("BILL", "SHOPS")).get(0).category()).isEqualTo("MARKETPLACES");
        assertThat(map(event.formatted("BILL", "MISC")).get(0).title()).isEqualTo("Перевод");
    }

    @Test
    void longPurposeIsCutToColumnLength() {
        String purpose = "Оплата по договору ".repeat(10);
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"88888888-8888-8888-8888-888888888888","type":"REQUISITES",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "recipientName":"ООО Ромашка","purpose":"%s","amount":1000,"commission":0,"total":1000,
                 "createdAt":"2026-11-03T10:00:00Z"}
                """.formatted(purpose));

        assertThat(ops.get(0).details()).hasSize(DETAILS_MAX).endsWith("…");
        assertThat(ops.get(0).category()).isEqualTo("REQUISITES");
    }

    @Test
    void ownTransferWithoutTitlesUsesNeutralWords() {
        List<NewOperation> ops = map("""
                {"eventType":"PaymentSucceeded","paymentId":"99999999-9999-9999-9999-999999999999","type":"OWN_TRANSFER",
                 "userId":"00000000-0000-0000-0000-000000000001","sourceAccountId":"00000000-0000-0000-0000-00000000a002",
                 "recipientUserId":"00000000-0000-0000-0000-000000000001",
                 "recipientAccountId":"00000000-0000-0000-0000-00000000a001","amount":10,"commission":0,"total":10,
                 "createdAt":"2026-11-03T10:00:00Z"}
                """);

        assertThat(ops.get(0).title()).isEqualTo("На свой счёт");
        assertThat(ops.get(1).title()).isEqualTo("С своего счёта");
    }
}
