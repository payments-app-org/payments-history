package com.paymentsapp.history.operation;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// Фильтры комбинируются как угодно, поэтому SQL собираем из кусочков; все значения — параметрами
@Repository
public class OperationRepository {

    private final JdbcClient jdbc;

    public OperationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Повтор события из Kafka (тот же платёж и пользователь) молча пропускается
    public boolean insert(NewOperation op) {
        return jdbc.sql("""
                        insert into operations (id, user_id, account_id, occurred_at, direction, category, title, details,
                            amount, fee, currency, amount_rub, cashback, loyalty_program, counterparty_user_id, payment_id)
                        values (:id, :userId, :accountId, :occurredAt, :direction, :category, :title, :details,
                            :amount, :fee, :currency, :amountRub, :cashback, :loyalty, :counterparty, :paymentId)
                        on conflict (payment_id, user_id) where payment_id is not null do nothing
                        """)
                .param("id", UUID.randomUUID())
                .param("userId", op.userId())
                .param("accountId", op.accountId())
                .param("occurredAt", op.occurredAt())
                .param("direction", op.direction().name())
                .param("category", op.category())
                .param("title", op.title())
                .param("details", op.details())
                .param("amount", op.amount())
                .param("fee", op.fee())
                .param("currency", op.currency())
                .param("amountRub", op.amountRub())
                .param("cashback", op.cashback())
                .param("loyalty", op.loyaltyProgram() == null ? null : op.loyaltyProgram().name())
                .param("counterparty", op.counterpartyUserId())
                .param("paymentId", op.paymentId())
                .update() == 1;
    }

    // Страница ленты: limit + 1 строк, чтобы понять, есть ли продолжение
    public List<Operation> find(OperationFilter filter, Cursor after, int limit) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder sql = new StringBuilder(
                "select o.*, c.name as category_name from operations o join categories c on c.code = o.category where ");
        sql.append(where(filter, params));
        if (after != null) {
            sql.append(" and (o.occurred_at, o.id) < (:cursorAt, :cursorId)");
            params.put("cursorAt", after.occurredAt());
            params.put("cursorId", after.id());
        }
        sql.append(" order by o.occurred_at desc, o.id desc limit :limit");
        params.put("limit", limit + 1);
        return jdbc.sql(sql.toString()).params(params).query(OperationRepository::mapOperation).list();
    }

    // Итоги по категориям, а внутри одной категории — по магазинам/контрагентам
    public List<SummaryGroup> summarize(OperationFilter filter, boolean byTitle) {
        Map<String, Object> params = new HashMap<>();
        String sql = byTitle
                ? "select o.title as key, o.title as name, sum(o.amount_rub) as total, count(*) as cnt"
                  + " from operations o where " + where(filter, params)
                  + " group by o.title order by total desc"
                : "select o.category as key, c.name as name, sum(o.amount_rub) as total, count(*) as cnt"
                  + " from operations o join categories c on c.code = o.category where " + where(filter, params)
                  + " group by o.category, c.name order by total desc";
        return jdbc.sql(sql).params(params)
                .query((rs, n) -> new SummaryGroup(rs.getString("key"), rs.getString("name"),
                        rs.getBigDecimal("total"), rs.getLong("cnt")))
                .list();
    }

    public List<Category> categories() {
        return jdbc.sql("select code, name from categories order by sort_order")
                .query((rs, n) -> new Category(rs.getString("code"), rs.getString("name")))
                .list();
    }

    private static String where(OperationFilter f, Map<String, Object> params) {
        StringBuilder w = new StringBuilder("o.user_id = :userId");
        params.put("userId", f.userId());
        if (f.accountId() != null) {
            w.append(" and o.account_id = :accountId");
            params.put("accountId", f.accountId());
        }
        if (f.from() != null) {
            w.append(" and o.occurred_at >= :from");
            params.put("from", f.from());
        }
        if (f.to() != null) {
            w.append(" and o.occurred_at < :to");
            params.put("to", f.to());
        }
        if (f.direction() != null) {
            w.append(" and o.direction = :direction");
            params.put("direction", f.direction().name());
        }
        if (f.categories() != null && !f.categories().isEmpty()) {
            w.append(" and o.category in (:categories)");
            params.put("categories", f.categories());
        }
        if (f.loyalty() != null && !f.loyalty().isEmpty()) {
            w.append(" and o.loyalty_program in (:loyalty)");
            params.put("loyalty", f.loyalty().stream().map(Enum::name).toList());
        }
        if (f.currencies() != null && !f.currencies().isEmpty()) {
            w.append(" and o.currency in (:currencies)");
            params.put("currencies", f.currencies());
        }
        if (f.counterpartyUserId() != null) {
            w.append(" and o.counterparty_user_id = :counterparty");
            params.put("counterparty", f.counterpartyUserId());
        }
        return w.toString();
    }

    private static Operation mapOperation(ResultSet rs, int n) throws SQLException {
        String loyalty = rs.getString("loyalty_program");
        return new Operation(
                rs.getObject("id", UUID.class),
                rs.getObject("account_id", UUID.class),
                rs.getObject("occurred_at", OffsetDateTime.class),
                Direction.valueOf(rs.getString("direction")),
                rs.getString("category"),
                rs.getString("category_name"),
                rs.getString("title"),
                rs.getString("details"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("fee"),
                rs.getString("currency"),
                rs.getBigDecimal("cashback"),
                loyalty == null ? null : LoyaltyProgram.valueOf(loyalty),
                rs.getObject("counterparty_user_id", UUID.class));
    }

    public record SummaryGroup(String key, String name, BigDecimal total, long count) {
    }

    public record Category(String code, String name) {
    }
}
