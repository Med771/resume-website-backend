package ru.ai.sin.logic.request;

import org.springframework.data.domain.Pageable;
import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.request.dto.*;


public interface RequestService {
    RequestDTO getById(long id);

    PageResponse<RequestDTO> getByFilter(Pageable pageable, FilterRequestReq filterRequestReq);

    PageResponse<RequestDTO> getMineByFilter(Pageable pageable, FilterRequestReq filterRequestReq);

    RequestDTO create(AddRequestReq addRequestReq);

    void studentRespond(long requestId, StudentRequestDecisionReq req);

    void deleteById(long id);
}
