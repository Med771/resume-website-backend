package ru.ai.sin.logic.request.dto;

import ru.ai.sin.models.enums.ResultEnum;

import java.util.List;
import java.util.UUID;

public record FilterRequestReq(
        List<ResultEnum> results,

        UUID recruiterId,
        UUID studentId
) {
}
