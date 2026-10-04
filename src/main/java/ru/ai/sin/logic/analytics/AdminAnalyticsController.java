package ru.ai.sin.logic.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.analytics.dto.AnalyticsFunnelDTO;
import ru.ai.sin.logic.analytics.dto.AnalyticsSummaryDTO;
import ru.ai.sin.logic.analytics.dto.AnalyticsSummaryReq;
import ru.ai.sin.logic.analytics.dto.EntityPopulationSummaryDTO;
import ru.ai.sin.logic.analytics.dto.EntityPopulationSummaryReq;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@Tag(
        name = "AdminAnalytics",
        description = """
                Агрегированные отчёты: события с фронта (`/public/analytics/events`) и **сводки по сущностям** (пользователи по ролям, студенты, рекрутеры).
                Доступно **только ADMIN** (JWT в cookie).""")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Сводка по путям за интервал времени",
            description = """
                    Возвращает список пар (`path`, `events`) — число событий по каждому `path` за полуинтервал **[from, to)**.

                    **200** — `AnalyticsSummaryDTO` с полем `byPath`.

                    **400** — `from` / `to` null или нарушение валидации.

                    **401/403** — нет входа или не админ.""")
    @PostMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> summary(@Valid @RequestBody AnalyticsSummaryReq req) {
        return ResponseEntity.ok(analyticsService.summarize(req));
    }

    @Operation(
            summary = "Сводка по количеству пользователей, студентов и рекрутеров",
            description = """
                    **200** — `EntityPopulationSummaryDTO`: всего пользователей и разбивка по ролям (`GUEST`, `USER`, `STUDENT`, `ADMIN`),
                    всего записей в `students` и `recruiters`.

                    Тело опционально: если переданы **`from`** и **`to`**, дополнительно возвращаются числа **новых** студентов и рекрутеров за **[from, to)** по полю `created_at` (у пользователей в таблице `users` даты создания нет — только срез по ролям).

                    **400** — задано только одно из полей окна, или `to` не строго после `from`.

                    **401/403** — нет входа или не админ.""")
    @PostMapping("/entity-population")
    public ResponseEntity<EntityPopulationSummaryDTO> entityPopulation(
            @Valid @RequestBody(required = false) EntityPopulationSummaryReq req) {
        EntityPopulationSummaryReq body = req == null ? new EntityPopulationSummaryReq(null, null) : req;
        return ResponseEntity.ok(analyticsService.summarizeEntityPopulation(body));
    }

    @Operation(summary = "Воронка по типам событий за интервал")
    @PostMapping("/funnel")
    public ResponseEntity<AnalyticsFunnelDTO> funnel(@Valid @RequestBody AnalyticsSummaryReq req) {
        return ResponseEntity.ok(analyticsService.summarizeFunnel(req));
    }
}
