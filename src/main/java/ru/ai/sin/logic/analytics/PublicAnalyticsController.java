package ru.ai.sin.logic.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.analytics.dto.AnalyticsEventInReq;

@RestController
@RequestMapping("/public/analytics")
@RequiredArgsConstructor
@Tag(
        name = "PublicAnalytics",
        description = """
                Приём событий first-party аналитики **без авторизации** (`permitAll`).
                События пишутся в PostgreSQL. Действует лимит запросов с одного IP за минуту (`app.analytics.rate-limit-per-ip-per-minute`);
                при превышении — **429**.""")
public class PublicAnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Записать одно событие",
            description = """
                    **204** — событие принято и сохранено.

                    **400** — невалидное тело (неизвестный `eventType`, пустые обязательные поля, превышение длины).

                    **429** — слишком много запросов с IP-адреса клиента за минуту (см. конфиг).

                    IP и User-Agent на стороне сервера могут использоваться для rate limit и агрегированной статистики (без обязательного хранения «как есть» — см. реализацию сервиса).""")
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ingest(@Valid @RequestBody AnalyticsEventInReq req, HttpServletRequest httpRequest) {
        analyticsService.recordEvent(req, httpRequest);
    }
}
