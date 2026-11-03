package com.paymentsapp.history.ingest;

import com.paymentsapp.history.operation.NewOperation;
import com.paymentsapp.history.operation.OperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

// Читает итоги платежей из Kafka. Доставка «хотя бы один раз» — дубли отсекает уникальный индекс (payment_id, user_id, direction).
@Component
public class PaymentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsListener.class);

    private final OperationRepository operations;
    private final JsonMapper json;

    public PaymentEventsListener(OperationRepository operations, JsonMapper json) {
        this.operations = operations;
        this.json = json;
    }

    @KafkaListener(topics = "payment.events")
    @Transactional
    public void onPaymentEvent(String message) {
        for (NewOperation op : PaymentEventMapper.toOperations(json.readTree(message))) {
            if (operations.insert(op)) {
                log.info("payment {}: операция {} для пользователя {}", op.paymentId(), op.direction(), op.userId());
            }
        }
    }
}
