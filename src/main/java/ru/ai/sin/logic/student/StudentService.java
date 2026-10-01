package ru.ai.sin.logic.student;

import org.springframework.data.domain.Pageable;

import org.springframework.web.multipart.MultipartFile;

import ru.ai.sin.models.PageResponse;
import ru.ai.sin.logic.student.dto.*;

import java.util.Optional;
import java.util.UUID;

public interface StudentService {

    // ---------- GET METHODS ----------
    StudentDTO getById(UUID id);

    Optional<StudentDTO> getLinkedForCurrentUser();

    // ---------- POST METHODS ----------
    void setPhoto(
            UUID id,
            MultipartFile file);

    PageResponse<StudentCardDTO> getAllCardsByFilter(
            Pageable pageable,
            FilterStudentReq filterStudentReq);
    PageResponse<StudentDTO> getAllByFilter(
            Pageable pageable,
            FilterStudentReq filterStudentReq);

    StudentDTO create(AddStudentReq addStudentReq);
    StudentDTO createExtended(CreateStudentExtendedReq createStudentExtendedReq);

    StudentDTO update(
            UUID id,
            UpdateStudentReq updateStudentReq);
    StudentDTO patch(
            UUID id,
            PatchStudentReq patchStudentReq);

    StudentDTO patchMe(PatchStudentMeReq req);

    void reorder(ReorderStudentsReq req);

    BulkStudentVisibilityResult bulkUpdateVisibility(BulkStudentVisibilityReq req);

    // ---------- DELETE METHODS ----------
    void deleteById(UUID id);
}
