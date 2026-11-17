package ru.ai.sin.logic.recruiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.recruiter.dto.PatchRecruiterReq;
import ru.ai.sin.logic.recruiter.dto.RecruiterDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.RecruiterTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceImplTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private RecruiterRepo recruiterRepo;
    @Mock
    private RecruiterTools recruiterTools;
    @Mock
    private UserTools userTools;
    @Mock
    private UserRepo userRepo;
    @Mock
    private FileHelper fileHelper;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private RecruiterDeletionService recruiterDeletionService;
    @Mock
    private MultipartFile file;

    private RecruiterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecruiterServiceImpl(
                recruiterRepo,
                Mappers.getMapper(RecruiterMapper.class),
                recruiterTools,
                userTools,
                userRepo,
                fileHelper,
                securityHelper,
                accountAccessHelper,
                recruiterDeletionService);
    }

    @Test
    void me_returnsAccountUsername() {
        RecruiterEnt recruiter = recruiter();
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "Bee Ann", "ann_user", "hash");
        user.setRecruiter(recruiter);
        when(userTools.findCurrentUserFetchingRecruiter()).thenReturn(Optional.of(user));

        RecruiterDTO dto = service.getLinkedForCurrentUser().orElseThrow();

        assertThat(dto.username()).isEqualTo("ann_user");
    }

    @Test
    void patchName_keepsUsername() {
        RecruiterEnt recruiter = recruiter();
        when(recruiterTools.getRecruiterOrThrow(ID)).thenReturn(recruiter);
        when(userRepo.findByRecruiter_Id(ID)).thenReturn(Optional.of(owner()));

        RecruiterDTO dto = service.patch(ID, new PatchRecruiterReq(
                null, null, "New", null, null, null, null, null, null, null));

        assertThat(dto.firstName()).isEqualTo("New");
        assertThat(dto.username()).isEqualTo("ann_user");
    }

    @Test
    void setPhoto_writesPath() {
        RecruiterEnt recruiter = recruiter();
        when(recruiterTools.getRecruiterOrThrow(ID)).thenReturn(recruiter);
        when(fileHelper.saveFile(file, ID.toString())).thenReturn("photo.jpg");

        service.setPhoto(ID, file);

        assertThat(recruiter.getImagePath()).isEqualTo("photo.jpg");
    }

    @Test
    void deletePhoto_clearsPath() throws Exception {
        RecruiterEnt recruiter = recruiter();
        recruiter.setImagePath("photo.jpg");
        when(recruiterTools.getRecruiterOrThrow(ID)).thenReturn(recruiter);

        service.deletePhoto(ID);

        assertThat(recruiter.getImagePath()).isNull();
        verify(fileHelper).deleteFile("photo.jpg");
    }

    @Test
    void setPhoto_foreignRecruiter_isForbidden() {
        doThrow(new ForbiddenException("Нет доступа к профилю рекрутера"))
                .when(accountAccessHelper).requireRecruiterOwnsProfile(ID);

        assertThatThrownBy(() -> service.setPhoto(ID, file))
                .isInstanceOf(ForbiddenException.class);
        verify(fileHelper, never()).saveFile(file, ID.toString());
    }

    private static RecruiterEnt recruiter() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(ID);
        recruiter.setCompanyName("ACME");
        return recruiter;
    }

    private static UserEnt owner() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "Bee Ann", "ann_user", "hash");
        user.setRecruiter(recruiter());
        return user;
    }
}
