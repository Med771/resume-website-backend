package ru.ai.sin.logic.main;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.version.StudentProfileVersionService;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;

import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MainServiceImpl implements MainService {

    private final FileHelper fileHelper;
    private final StudentRepo studentRepo;
    private final RecruiterRepo recruiterRepo;
    private final UserRepo userRepo;
    private final AccountAccessHelper accountAccessHelper;
    private final SecurityHelper securityHelper;
    private final StudentProfileVersionService profileVersions;

    @Override
    public byte[] getFileContent(String fileName) {
        requireCanRead(fileName);
        try {
            return fileHelper.getFileContent(fileName);
        } catch (FileNotFoundException e) {
            throw new NotFoundException("File not found");
        }
    }

    private void requireCanRead(String fileName) {
        Optional<StudentEnt> student = fileName == null
                ? Optional.empty()
                : studentRepo.findByImagePath(fileName);
        if (student.isPresent()) {
            accountAccessHelper.requireCanReadStudentResumeDetails(student.get().getId());
            return;
        }
        java.util.Optional<java.util.UUID> draftOwner = profileVersions == null
                ? java.util.Optional.empty()
                : profileVersions.findStudentIdByImage(fileName);
        if (draftOwner != null && draftOwner.isPresent()) {
            if (!profileVersions.canReadDraftImage(draftOwner.get())) {
                throw new NotFoundException("File not found");
            }
            return;
        }
        Optional<RecruiterEnt> recruiter = fileName == null
                ? Optional.empty()
                : recruiterRepo.findByImagePath(fileName);
        if (recruiter != null && recruiter.isPresent()) {
            if (canReadRecruiterPhoto(recruiter.get().getId())) {
                return;
            }
            throw new NotFoundException("File not found");
        }
        if (!securityHelper.isCurrentUserAdmin()) {
            throw new NotFoundException("File not found");
        }
    }

    private boolean canReadRecruiterPhoto(UUID recruiterId) {
        if (securityHelper.isCurrentUserAdmin()) {
            return true;
        }
        String username = securityHelper.getCurrentUsernameOptional().orElse(null);
        if (username == null || recruiterId == null) {
            return false;
        }
        UserEnt owner = userRepo.findByRecruiter_Id(recruiterId).orElse(null);
        return owner != null && username.equals(owner.getUsername());
    }

    @Override
    public String getContentType(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "application/octet-stream";
        }
        String lower = imagePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (lower.endsWith(".avif")) {
            return "image/avif";
        }
        if (lower.endsWith(".heic") || lower.endsWith(".heif")) {
            return "image/heic";
        }
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
            return "image/tiff";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".jpe")
                || lower.endsWith(".jfif") || lower.endsWith(".pjpeg") || lower.endsWith(".pjp")) {
            return "image/jpeg";
        }

        return "image/jpeg";
    }
}
