package ru.ai.sin.logic.recruiter;

import org.mapstruct.*;

import ru.ai.sin.logic.recruiter.dto.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecruiterMapper {

    // ---------------- AddRecruiterReq -> RecruiterEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "userInformation.firstName", source = "firstName")
    @Mapping(target = "userInformation.lastName", source = "lastName")
    @Mapping(target = "userInformation.email", source = "email")
    @Mapping(target = "contactInformation.telegramUserId", ignore = true)
    @Mapping(target = "contactInformation.phoneNumber", source = "phoneNumber")
    @Mapping(target = "contactInformation.telegramUsername", source = "telegramUsername")
    RecruiterEnt toEntity(AddRecruiterReq dto);

    // ---------------- RecruiterEnt -> RecruiterDTO ----------------
    @Mapping(source = "entity.id", target = "id")
    @Mapping(source = "entity.companyName", target = "companyName")
    @Mapping(source = "entity.city", target = "city")
    @Mapping(source = "entity.userInformation.firstName", target = "firstName")
    @Mapping(source = "entity.userInformation.lastName", target = "lastName")
    @Mapping(source = "entity.userInformation.email", target = "email")
    @Mapping(source = "entity.contactInformation.phoneNumber", target = "phoneNumber")
    @Mapping(source = "entity.contactInformation.telegramUsername", target = "telegramUsername")
    @Mapping(source = "entity.contactInformation.telegramUserId", target = "telegramUserId")
    @Mapping(source = "entity.middleName", target = "middleName")
    @Mapping(source = "entity.cooperationFormat", target = "cooperationFormat")
    @Mapping(source = "entity.readyToRequestStudents", target = "readyToRequestStudents")
    @Mapping(source = "entity.imagePath", target = "imagePath")
    @Mapping(target = "username", source = "login")
    RecruiterDTO toDTO(RecruiterEnt entity, String login);

    // ---------------- UpdateRecruiterReq -> RecruiterEnt ----------------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "userInformation.firstName", source = "firstName")
    @Mapping(target = "userInformation.lastName", source = "lastName")
    @Mapping(target = "userInformation.email", source = "email")
    @Mapping(target = "contactInformation.telegramUserId", ignore = true)
    @Mapping(target = "contactInformation.phoneNumber", source = "phoneNumber")
    @Mapping(target = "contactInformation.telegramUsername", source = "telegramUsername")
    void updateEntityFromDto(UpdateRecruiterReq dto, @MappingTarget RecruiterEnt entity);

    // ---------------- PatchRecruiterReq -> RecruiterEnt ----------------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "city", source = "city")
    @Mapping(target = "userInformation.firstName", source = "firstName")
    @Mapping(target = "userInformation.lastName", source = "lastName")
    @Mapping(target = "userInformation.email", source = "email")
    @Mapping(target = "contactInformation.telegramUserId", ignore = true)
    @Mapping(target = "contactInformation.phoneNumber", source = "phoneNumber")
    @Mapping(target = "contactInformation.telegramUsername", source = "telegramUsername")
    @Mapping(target = "middleName", source = "middleName")
    void patchEntityFromDto(PatchRecruiterReq dto, @MappingTarget RecruiterEnt entity);

}
