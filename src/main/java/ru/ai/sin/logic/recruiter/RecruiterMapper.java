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
    @Mapping(source = "city", target = "city")
    @Mapping(source = "userInformation.firstName", target = "firstName")
    @Mapping(source = "userInformation.lastName", target = "lastName")
    @Mapping(source = "userInformation.email", target = "email")
    @Mapping(source = "contactInformation.phoneNumber", target = "phoneNumber")
    @Mapping(source = "contactInformation.telegramUsername", target = "telegramUsername")
    @Mapping(source = "contactInformation.telegramUserId", target = "telegramUserId")
    RecruiterDTO toDTO(RecruiterEnt entity);

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
    void patchEntityFromDto(PatchRecruiterReq dto, @MappingTarget RecruiterEnt entity);

}
