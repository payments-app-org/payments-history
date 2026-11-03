package com.paymentsapp.history.web;

import com.paymentsapp.history.operation.Cursor;
import com.paymentsapp.history.operation.Direction;
import com.paymentsapp.history.operation.LoyaltyProgram;
import com.paymentsapp.history.operation.Operation;
import com.paymentsapp.history.operation.OperationFilter;
import com.paymentsapp.history.operation.OperationRepository;
import com.paymentsapp.history.operation.OperationRepository.Category;
import com.paymentsapp.history.operation.OperationRepository.SummaryGroup;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Авторизации пока нет: пользователя передают в X-User-Id. Даты — по московскому времени.
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final int MAX_LIMIT = 100;

    private final OperationRepository operations;

    public HistoryController(OperationRepository operations) {
        this.operations = operations;
    }

    // Лента истории с фильтрами и пагинацией по курсору: следующая страница — ?cursor=<nextCursor>
    @GetMapping("/operations")
    public OperationsPage operations(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<LoyaltyProgram> loyalty,
            @RequestParam(required = false) List<String> currency,
            @RequestParam(required = false) UUID counterpartyId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        int size = Math.clamp(limit, 1, MAX_LIMIT);
        OperationFilter filter = new OperationFilter(userId, accountId, startOf(from),
                to == null ? null : startOf(to.plusDays(1)), direction, category, loyalty, currency, counterpartyId);
        List<Operation> rows = operations.find(filter, cursor == null ? null : Cursor.decode(cursor), size);
        boolean more = rows.size() > size;
        List<Operation> page = more ? rows.subList(0, size) : rows;
        String next = more ? new Cursor(page.getLast().occurredAt(), page.getLast().id()).encode() : null;
        return new OperationsPage(page, next);
    }

    // Расходы или доходы за месяц (для мобильного приложения): итог и разбивка по категориям.
    // С category — разбивка внутри категории по магазинам/контрагентам. Сами операции — через /operations с теми же фильтрами.
    @GetMapping("/summary")
    public Summary summary(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(defaultValue = "EXPENSE") Direction direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) List<String> currency) {
        YearMonth m = month != null ? month : YearMonth.now(ZONE);
        OperationFilter filter = new OperationFilter(userId, accountId, startOf(m.atDay(1)),
                startOf(m.plusMonths(1).atDay(1)), direction, category == null ? null : List.of(category), null,
                currency, null);
        List<SummaryGroup> groups = operations.summarize(filter, category != null);
        BigDecimal total = groups.stream().map(SummaryGroup::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = groups.stream().mapToLong(SummaryGroup::count).sum();
        return new Summary(m.toString(), direction, category, total, count, groups,
                m.minusMonths(1).toString(), m.plusMonths(1).toString());
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return operations.categories();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private static OffsetDateTime startOf(LocalDate day) {
        return day == null ? null : day.atStartOfDay(ZONE).toOffsetDateTime();
    }

    public record OperationsPage(List<Operation> items, String nextCursor) {
    }

    // Суммы — в рублях (операции в валюте пересчитаны по курсу на момент операции)
    public record Summary(String month, Direction direction, String category, BigDecimal total, long count,
                          List<SummaryGroup> groups, String previousMonth, String nextMonth) {
    }
}
