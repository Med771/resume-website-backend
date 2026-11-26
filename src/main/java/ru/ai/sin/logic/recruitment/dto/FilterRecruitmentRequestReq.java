package ru.ai.sin.logic.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;

@Schema(name = "FilterRecruitmentRequestReq", description = "Фильтр списка заявок. Пустой статус ничего не отсекает. Студенту статус из тела не отдаёт закрытые и архивные заявки.")
public record FilterRecruitmentRequestReq(
        @Schema(description = "OPEN, CLOSED или ARCHIVED. Пусто — без ограничения для админа и владельца")
        RecruitmentRequestStatus status
) {
}
