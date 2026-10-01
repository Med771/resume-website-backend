package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

import java.util.List;
import java.util.UUID;

@Schema(name = "BulkStudentVisibilityReq")
public record BulkStudentVisibilityReq(
        @Schema(description = "Конкретные ID студентов; если пусто и all=true — ко всем подходящим")
        List<UUID> studentIds,

        @Schema(description = "Применить ко всем студентам (игнорируется, если studentIds не пуст)")
        Boolean all,

        @Schema(description = "Видимость в каталоге для рекрутёров")
        Boolean catalogVisible,

        @Schema(description = "Показ на главной витрине (анонимы)")
        Boolean publicProfileConsent,

        @Schema(description = "Только аккаунты со статусом APPROVED (по умолчанию true)")
        Boolean onlyApprovedAccounts
) {
    @AssertTrue(message = "Укажите catalogVisible и/или publicProfileConsent")
    public boolean hasVisibilityField() {
        return catalogVisible != null || publicProfileConsent != null;
    }

    public boolean effectiveOnlyApproved() {
        return onlyApprovedAccounts == null || onlyApprovedAccounts;
    }

    public boolean effectiveAll() {
        return Boolean.TRUE.equals(all);
    }
}
