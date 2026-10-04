package ru.ai.sin.logic.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.profile.dto.CommunicationReadinessRes;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Профиль текущего пользователя")
public class ProfileController {

    private final ProfileCommunicationGateService profileCommunicationGateService;

    @Operation(summary = "Готовность профиля к переписке")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/communication-readiness")
    public ResponseEntity<CommunicationReadinessRes> communicationReadiness() {
        return ResponseEntity.ok(profileCommunicationGateService.evaluateForCurrentUser());
    }
}
