package ru.ai.sin.logic.student;

import org.mapstruct.*;

import ru.ai.sin.logic.skill.dto.SkillDTO;

import ru.ai.sin.logic.student.dto.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StudentMapper {

    // ---------------- AddStudentReq -> StudentEnt ----------------
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "speciality",  ignore = true)
    @Mapping(target = "portfolio", ignore = true)
    @Mapping(target = "education", ignore = true)
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "publicProfileConsent", ignore = true)
    @Mapping(target = "profileTextScore", ignore = true)
    @Mapping(target = "manualSortOrder", ignore = true)
    @Mapping(target = "userInformation.firstName", source = "firstName")
    @Mapping(target = "userInformation.lastName", source = "lastName")
    @Mapping(target = "userInformation.email", source = "email")
    @Mapping(target = "contactInformation.phoneNumber", source = "phoneNumber")
    @Mapping(target = "contactInformation.telegramUsername", source = "telegramUsername")
    StudentEnt toEntity(AddStudentReq studentReq);

    // ---------------- StudentEnt -> StudentDTO ----------------
    @Mapping(source = "student.userInformation.firstName", target = "firstName")
    @Mapping(source = "student.userInformation.lastName", target = "lastName")
    @Mapping(source = "student.middleName", target = "middleName")
    @Mapping(source = "student.gender", target = "gender")
    @Mapping(source = "student.userInformation.email", target = "email")
    @Mapping(source = "student.contactInformation.phoneNumber", target = "phoneNumber")
    @Mapping(source = "student.contactInformation.telegramUsername", target = "telegramUsername")
    @Mapping(source = "student.speciality.id", target = "specialityId")
    @Mapping(source = "student.speciality.name", target = "speciality")
    @Mapping(source = "student.publicProfileConsent", target = "publicProfileConsent")
    @Mapping(source = "student.catalogVisible", target = "catalogVisible")
    @Mapping(source = "student.profileTextScore", target = "profileTextScore")
    @Mapping(source = "student.manualSortOrder", target = "manualSortOrder")
    StudentDTO toDTO(StudentEnt student, List<SkillDTO> skills);

    // ---------------- StudentEnt -> StudentCardDTO ----------------
    @Mapping(source = "student.userInformation.firstName", target = "firstName")
    @Mapping(source = "student.userInformation.lastName", target = "lastName")
    @Mapping(source = "student.middleName", target = "middleName")
    @Mapping(source = "student.speciality.name", target = "speciality")
    @Mapping(source = "student.manualSortOrder", target = "manualSortOrder")
    StudentCardDTO toCardDTO(StudentEnt student, List<SkillDTO> skills);

    // ---------------- UpdateStudentReq -> StudentEnt ----------------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student.contactInformation.telegramUserId", ignore = true)
    @Mapping(target = "timestamps", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "speciality",  ignore = true)
    @Mapping(target = "portfolio", ignore = true)
    @Mapping(target = "education", ignore = true)
    @Mapping(target = "companies", ignore = true)
    @Mapping(target = "publicProfileConsent", ignore = true)
    @Mapping(target = "profileTextScore", ignore = true)
    @Mapping(target = "manualSortOrder", ignore = true)
    void updateEntityFromDto(UpdateStudentReq updateStudentReq, @MappingTarget StudentEnt student);
}
