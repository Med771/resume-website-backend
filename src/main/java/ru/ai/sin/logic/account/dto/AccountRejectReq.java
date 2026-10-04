package ru.ai.sin.logic.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "AccountRejectReq")
public record AccountRejectReq(
        @Size(max = 2000) String comment
) {
}
