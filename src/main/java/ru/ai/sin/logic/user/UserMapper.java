package ru.ai.sin.logic.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ru.ai.sin.logic.user.dto.UserDTO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "recruiterId", source = "recruiter.id")
    @Mapping(target = "studentId", source = "student.id")
    UserDTO toDTO(UserEnt entity);
}
