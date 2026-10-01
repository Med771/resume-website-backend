package ru.ai.sin.logic.user;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.logic.user.dto.AddUserReq;
import ru.ai.sin.logic.user.dto.FilterUserReq;
import ru.ai.sin.logic.user.dto.UserDTO;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

public interface UserService {

    PageResponse<UserDTO> getByFilter(Pageable pageable, FilterUserReq filterUserReq);

    UserDTO create(AddUserReq addUserReq);

    void deleteById(UUID id);
}
