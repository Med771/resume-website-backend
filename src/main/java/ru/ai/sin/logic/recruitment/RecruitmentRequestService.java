package ru.ai.sin.logic.recruitment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.ConflictException;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.city.CityRepo;
import ru.ai.sin.logic.recruitment.dto.FilterRecruitmentRequestReq;
import ru.ai.sin.logic.recruitment.dto.RecruitmentRequestDTO;
import ru.ai.sin.logic.recruitment.dto.RecruitmentResponseDTO;
import ru.ai.sin.logic.recruitment.dto.UpsertRecruitmentRequestReq;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.speciality.SpecialityRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.version.StudentProfileVersionService;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;
import ru.ai.sin.models.enums.RecruitmentResponseStatus;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.UserTools;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecruitmentRequestService {

    private final RecruitmentRequestRepo requestRepo;
    private final RecruitmentRequestSpecialityRepo specialityLinkRepo;
    private final RecruitmentRequestCityRepo cityLinkRepo;
    private final RecruitmentResponseRepo responseRepo;
    private final SpecialityRepo specialityRepo;
    private final CityRepo cityRepo;
    private final RecruiterRepo recruiterRepo;
    private final StudentRepo studentRepo;
    private final UserTools userTools;
    private final StudentProfileVersionService profileVersions;

    @Transactional
    public RecruitmentRequestDTO create(UpsertRecruitmentRequestReq req) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = new RecruitmentRequestEnt();
        entity.setStatus(RecruitmentRequestStatus.OPEN);
        applyOwner(entity, user, req.companyName());
        applyContent(entity, req);
        RecruitmentRequestEnt saved = requestRepo.save(entity);
        replaceLinks(saved.getId(), req.specialityIds(), req.cityIds());
        return toDto(saved);
    }

    @Transactional
    public RecruitmentRequestDTO update(UUID id, UpsertRecruitmentRequestReq req) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = load(id);
        requireOwnerOrAdmin(user, entity);
        if (entity.getStatus() == RecruitmentRequestStatus.ARCHIVED) {
            throw new BadRequestException("Архивную заявку нельзя менять");
        }
        if (user.getRole() == RoleEnum.ADMIN) {
            entity.setCompanyName(requireCompanyName(req.companyName()));
        }
        applyContent(entity, req);
        replaceLinks(entity.getId(), req.specialityIds(), req.cityIds());
        return toDto(requestRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public RecruitmentRequestDTO getById(UUID id) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = load(id);
        assertCanRead(user, entity);
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecruitmentRequestDTO> filter(org.springframework.data.domain.Pageable pageable, FilterRecruitmentRequestReq req) {
        UserEnt user = currentUser();
        RecruitmentRequestStatus status = req == null ? null : req.status();
        var spec = switch (user.getRole()) {
            case ADMIN -> RecruitmentRequestSpecifications.forAdmin(status);
            case RECRUITER -> RecruitmentRequestSpecifications.forRecruiter(requireRecruiterId(user), status);
            case STUDENT -> {
                requirePublishedStudent(user);
                yield RecruitmentRequestSpecifications.openInWindow(LocalDateTime.now());
            }
        };
        PageRequest page = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamps.createdAt"));
        Page<RecruitmentRequestEnt> result = requestRepo.findAll(spec, page);
        return new PageResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                page.getPageNumber(),
                page.getPageSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public void close(UUID id) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = load(id);
        requireOwnerOrAdmin(user, entity);
        if (entity.getStatus() != RecruitmentRequestStatus.OPEN) {
            throw new BadRequestException("Закрыть можно только открытую заявку");
        }
        entity.setStatus(RecruitmentRequestStatus.CLOSED);
        requestRepo.save(entity);
    }

    @Transactional
    public void archive(UUID id) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = load(id);
        requireOwnerOrAdmin(user, entity);
        if (entity.getStatus() == RecruitmentRequestStatus.ARCHIVED) {
            throw new BadRequestException("Заявка уже в архиве");
        }
        entity.setStatus(RecruitmentRequestStatus.ARCHIVED);
        requestRepo.save(entity);
    }

    @Transactional
    public RecruitmentResponseDTO respond(UUID requestId) {
        UserEnt user = currentUser();
        requirePublishedStudent(user);
        RecruitmentRequestEnt entity = load(requestId);
        if (!visibleToStudent(entity, LocalDateTime.now())) {
            throw new NotFoundException("Заявка не найдена");
        }
        UUID studentId = user.getStudent().getId();
        if (responseRepo.existsByRecruitmentRequestIdAndStudentId(requestId, studentId)) {
            throw new ConflictException("Отклик на эту заявку уже есть");
        }
        RecruitmentResponseEnt response = new RecruitmentResponseEnt();
        response.setRecruitmentRequestId(requestId);
        response.setStudentId(studentId);
        response.setStatus(RecruitmentResponseStatus.PENDING);
        return toResponseDto(responseRepo.save(response));
    }

    @Transactional(readOnly = true)
    public List<RecruitmentResponseDTO> listResponses(UUID requestId) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = load(requestId);
        if (user.getRole() == RoleEnum.STUDENT) {
            requirePublishedStudent(user);
            assertCanRead(user, entity);
            return responseRepo.findByRecruitmentRequestIdAndStudentId(requestId, user.getStudent().getId())
                    .map(row -> List.of(toResponseDto(row)))
                    .orElseGet(List::of);
        }
        requireOwnerOrAdmin(user, entity);
        return responseRepo.findByRecruitmentRequestIdOrderByTimestampsCreatedAtAsc(requestId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public RecruitmentResponseDTO accept(UUID requestId, UUID responseId) {
        return decide(requestId, responseId, RecruitmentResponseStatus.ACCEPTED);
    }

    @Transactional
    public RecruitmentResponseDTO reject(UUID requestId, UUID responseId) {
        return decide(requestId, responseId, RecruitmentResponseStatus.REJECTED);
    }

    private RecruitmentResponseDTO decide(UUID requestId, UUID responseId, RecruitmentResponseStatus status) {
        UserEnt user = currentUser();
        RecruitmentRequestEnt entity = load(requestId);
        requireOwnerOrAdmin(user, entity);
        RecruitmentResponseEnt response = responseRepo.findByIdAndRecruitmentRequestId(responseId, requestId)
                .orElseThrow(() -> new NotFoundException("Отклик не найден"));
        response.setStatus(status);
        return toResponseDto(responseRepo.save(response));
    }

    private void applyOwner(RecruitmentRequestEnt entity, UserEnt user, String companyName) {
        if (user.getRole() == RoleEnum.ADMIN) {
            entity.setRecruiterId(null);
            entity.setCompanyName(requireCompanyName(companyName));
            return;
        }
        if (user.getRole() != RoleEnum.RECRUITER || user.getAccountStatus() != AccountStatus.APPROVED) {
            throw new ForbiddenException("Заявку может создать только одобренный работодатель");
        }
        entity.setRecruiterId(requireRecruiterId(user));
        entity.setCompanyName(null);
    }

    private void applyContent(RecruitmentRequestEnt entity, UpsertRecruitmentRequestReq req) {
        if (req.publishedFrom() != null && req.publishedTo() != null && req.publishedFrom().isAfter(req.publishedTo())) {
            throw new BadRequestException("Начало публикации позже её конца");
        }
        entity.setTasks(req.tasks().trim());
        entity.setQualificationRequirements(req.qualificationRequirements().trim());
        entity.setWorkConditions(req.workConditions().trim());
        entity.setStudentCount(req.studentCount());
        entity.setCooperationFormat(req.cooperationFormat());
        entity.setReadyToRequestStudents(req.readyToRequestStudents());
        entity.setScheduleNote(blankToNull(req.scheduleNote()));
        entity.setExtraInfo(blankToNull(req.extraInfo()));
        entity.setPublishedFrom(req.publishedFrom());
        entity.setPublishedTo(req.publishedTo());
    }

    private void replaceLinks(UUID requestId, List<Long> specialityIds, List<Long> cityIds) {
        List<Long> specialities = uniqueIds(specialityIds, "специальностей");
        List<Long> cities = uniqueIds(cityIds, "городов");
        if (specialityRepo.findAllById(specialities).size() != specialities.size()) {
            throw new NotFoundException("Некоторые специальности не найдены");
        }
        if (cityRepo.findAllById(cities).size() != cities.size()) {
            throw new NotFoundException("Некоторые города не найдены");
        }
        specialityLinkRepo.deleteByRecruitmentRequestId(requestId);
        cityLinkRepo.deleteByRecruitmentRequestId(requestId);
        specialityLinkRepo.saveAll(specialities.stream().map(id -> linkSpeciality(requestId, id)).toList());
        cityLinkRepo.saveAll(cities.stream().map(id -> linkCity(requestId, id)).toList());
    }

    private static RecruitmentRequestSpecialityEnt linkSpeciality(UUID requestId, Long specialityId) {
        RecruitmentRequestSpecialityEnt link = new RecruitmentRequestSpecialityEnt();
        link.setRecruitmentRequestId(requestId);
        link.setSpecialityId(specialityId);
        return link;
    }

    private static RecruitmentRequestCityEnt linkCity(UUID requestId, Long cityId) {
        RecruitmentRequestCityEnt link = new RecruitmentRequestCityEnt();
        link.setRecruitmentRequestId(requestId);
        link.setCityId(cityId);
        return link;
    }

    private static List<Long> uniqueIds(List<Long> ids, String name) {
        Set<Long> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new BadRequestException("В списке " + name + " есть повторы");
        }
        return ids;
    }

    private void assertCanRead(UserEnt user, RecruitmentRequestEnt entity) {
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (user.getRole() == RoleEnum.RECRUITER) {
            if (!requireRecruiterId(user).equals(entity.getRecruiterId())) {
                throw new NotFoundException("Заявка не найдена");
            }
            return;
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            requirePublishedStudent(user);
            if (!visibleToStudent(entity, LocalDateTime.now())) {
                throw new NotFoundException("Заявка не найдена");
            }
            return;
        }
        throw new ForbiddenException("Нет доступа к заявке");
    }

    private void requireOwnerOrAdmin(UserEnt user, RecruitmentRequestEnt entity) {
        if (user.getRole() == RoleEnum.ADMIN) {
            return;
        }
        if (user.getRole() == RoleEnum.RECRUITER && requireRecruiterId(user).equals(entity.getRecruiterId())) {
            return;
        }
        throw new NotFoundException("Заявка не найдена");
    }

    private void requirePublishedStudent(UserEnt user) {
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null
                || !profileVersions.hasPublishedVersion(user.getStudent().getId())) {
            throw new ForbiddenException("Заявки доступны после одобрения анкеты");
        }
    }

    private static boolean visibleToStudent(RecruitmentRequestEnt entity, LocalDateTime now) {
        if (entity.getStatus() != RecruitmentRequestStatus.OPEN) {
            return false;
        }
        if (entity.getPublishedFrom() != null && entity.getPublishedFrom().isAfter(now)) {
            return false;
        }
        return entity.getPublishedTo() == null || !entity.getPublishedTo().isBefore(now);
    }

    private UUID requireRecruiterId(UserEnt user) {
        if (user.getRecruiter() == null || user.getRecruiter().getId() == null) {
            throw new ForbiddenException("К аккаунту не привязана карточка работодателя");
        }
        return user.getRecruiter().getId();
    }

    private static String requireCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new BadRequestException("Укажите название компании");
        }
        return companyName.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private UserEnt currentUser() {
        return userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
    }

    private RecruitmentRequestEnt load(UUID id) {
        return requestRepo.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private RecruitmentRequestDTO toDto(RecruitmentRequestEnt entity) {
        String companyName = entity.getCompanyName();
        if (entity.getRecruiterId() != null) {
            companyName = recruiterRepo.findById(entity.getRecruiterId())
                    .map(RecruiterEnt::getCompanyName)
                    .orElse(companyName);
        }
        List<Long> specialityIds = specialityLinkRepo.findByRecruitmentRequestId(entity.getId()).stream()
                .map(RecruitmentRequestSpecialityEnt::getSpecialityId)
                .sorted()
                .toList();
        List<Long> cityIds = cityLinkRepo.findByRecruitmentRequestId(entity.getId()).stream()
                .map(RecruitmentRequestCityEnt::getCityId)
                .sorted()
                .toList();
        return new RecruitmentRequestDTO(
                entity.getId(),
                entity.getRecruiterId(),
                companyName,
                entity.getTasks(),
                entity.getQualificationRequirements(),
                entity.getWorkConditions(),
                entity.getStudentCount(),
                entity.getCooperationFormat(),
                entity.getReadyToRequestStudents(),
                entity.getScheduleNote(),
                entity.getExtraInfo(),
                entity.getPublishedFrom(),
                entity.getPublishedTo(),
                entity.getStatus(),
                specialityIds,
                cityIds);
    }

    private RecruitmentResponseDTO toResponseDto(RecruitmentResponseEnt response) {
        StudentEnt student = studentRepo.findById(response.getStudentId()).orElse(null);
        String firstName = null;
        String lastName = null;
        String speciality = null;
        if (student != null) {
            if (student.getUserInformation() != null) {
                firstName = student.getUserInformation().getFirstName();
                lastName = student.getUserInformation().getLastName();
            }
            if (student.getSpeciality() != null) {
                speciality = student.getSpeciality().getName();
            }
        }
        return new RecruitmentResponseDTO(
                response.getId(),
                response.getRecruitmentRequestId(),
                response.getStudentId(),
                firstName,
                lastName,
                speciality,
                response.getStatus());
    }
}
