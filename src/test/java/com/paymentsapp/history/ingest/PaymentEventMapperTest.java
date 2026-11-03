package com.paymentsapp.history.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import com.paymentsapp.history.operation.Direction;
import com.paymentsapp.history.operation.NewOperation;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PaymentEventMapperTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private List<NewOperation> map(String event) {
        return PaymentEventMapper.toOperations(json.readTree(event));
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
                 "senderName":"С Зарплатная WEEKEND ·· 3752","recipientUserId":"00000000-0000-0000-0000-000000000001",
                 "recipientAccountId":"00000000-0000-0000-0000-00000000a001","recipientName":"На Мой кошелёк",
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
}
