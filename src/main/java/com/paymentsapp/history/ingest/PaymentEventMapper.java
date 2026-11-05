package com.paymentsapp.history.ingest;

import com.paymentsapp.history.config.HistoryProperties;
import com.paymentsapp.history.operation.CategoryCode;
import com.paymentsapp.history.operation.Direction;
import com.paymentsapp.history.operation.NewOperation;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

// Успешный платёж → операции в истории: списание у отправителя и, если получатель наш клиент, поступление у него.
// Событие несёт данные (имена, названия счетов, суммы), тексты для ленты собирает history.
// Между своими счетами обе операции у одного пользователя: «На Мой кошелёк» и «С Зарплатная ·· 3752».
@Component
public class PaymentEventMapper {

    private static final String ELLIPSIS = "…";
    private static final Locale RU = Locale.of("ru");

    private final HistoryProperties props;

    public PaymentEventMapper(HistoryProperties props) {
        this.props = props;
    }

    public List<NewOperation> toOperations(PaymentEvent e) {
        if (!e.succeeded()) {
            return List.of();
        }
        CategoryCode category = PaymentCategories.of(e.type(), e.providerGroup());
        boolean own = category == CategoryCode.OWN_TRANSFER;
        String currency = e.currencyOrDefault();
        String details = details(e, category);

        // Отправитель видит получателя: имя, если это наш клиент, иначе телефон или маску карты
        String recipientTitle = own ? OperationText.TO_OWN.format(orElse(e.recipientName(), OperationText.OWN_DESTINATION))
                : firstNonBlank(e.recipientName(), e.recipientPhone(), e.recipientCardMask(), OperationText.TRANSFER.text());
        // «Округление на пользу» — отдельной операцией: покупка без него, разница — в фонд
        BigDecimal roundUp = e.roundUp();
        BigDecimal purchase = e.total().subtract(roundUp);

        List<NewOperation> ops = new ArrayList<>();
        ops.add(new NewOperation(e.userId(), e.sourceAccountId(), e.at(), Direction.EXPENSE, category.name(), recipientTitle,
                details, purchase, e.commission(), currency, purchase, null, null, e.recipientUserId(), e.paymentId()));
        if (roundUp.signum() > 0) {
            ops.add(new NewOperation(e.userId(), e.sourceAccountId(), e.at(), Direction.EXPENSE, CategoryCode.DONATIONS.name(),
                    firstNonBlank(e.roundUpFundName(), OperationText.ROUND_UP.text()),
                    OperationText.ROUND_UP_DETAILS.format(recipientTitle), roundUp, BigDecimal.ZERO, currency, roundUp,
                    null, null, null, e.paymentId()));
        }
        // Деньги, ушедшие на свою карту другого банка, в нашей истории не «приходят» — там только списание
        if (e.recipientUserId() != null && !e.toExternalAccount()) {
            String senderTitle = own ? OperationText.FROM_OWN.format(orElse(e.senderName(), OperationText.OWN_SOURCE))
                    : firstNonBlank(e.senderName(), OperationText.TRANSFER.text());
            ops.add(new NewOperation(e.recipientUserId(), e.recipientAccountId(), e.at(), Direction.INCOME, category.name(),
                    senderTitle, details, e.amount(), BigDecimal.ZERO, currency, e.amount(), null, null, e.userId(),
                    e.paymentId()));
        }
        return ops;
    }

    // Пометка: сообщение получателю; юрлицу и в бюджет — назначение платежа; за рубеж — сколько и как получит получатель
    private String details(PaymentEvent e, CategoryCode category) {
        if (category == CategoryCode.ABROAD_TRANSFER && e.receiveAmount() != null) {
            return OperationText.ABROAD_DETAILS.format(amount(e.receiveAmount()), e.receiveCurrency(), e.transferMethodName());
        }
        String message = blankToNull(e.message());
        String purpose = blankToNull(e.purpose());
        if (message == null && purpose != null) {
            return purpose.length() > props.detailsMax() ? purpose.substring(0, props.detailsMax() - 1) + ELLIPSIS : purpose;
        }
        return message;
    }

    // «1,75» — без лишних нулей, запятая
    private static String amount(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(RU));
        return format.format(value);
    }

    private static String orElse(String value, OperationText fallback) {
        return firstNonBlank(value, fallback.text());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
