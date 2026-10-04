package ru.ai.sin.logic.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.verification.dto.PhoneVerificationConfirmCodeReq;
import ru.ai.sin.logic.verification.dto.PhoneVerificationStartReq;
import ru.ai.sin.logic.verification.dto.PhoneVerificationStartRes;
import ru.ai.sin.logic.verification.dto.PhoneVerificationStatusRes;

import java.util.UUID;

@RestController
@RequestMapping("/verification/phone")
@RequiredArgsConstructor
@Tag(name = "PhoneVerification", description = "Подтверждение телефона: Telegram-бот, OTP на почту или тестовый код")
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @Operation(summary = "Начать верификацию телефона",
            description = "Возвращает ссылку на Telegram-бота. При передаче email и включённой почте — отправляет OTP на почту.")
    @PostMapping("/start")
    public ResponseEntity<PhoneVerificationStartRes> start(@Valid @RequestBody PhoneVerificationStartReq req) {
        return ResponseEntity.ok(phoneVerificationService.startVerification(req));
    }

    @Operation(summary = "Статус верификации", description = "PENDING / CONFIRMED / EXPIRED")
    @GetMapping("/{verificationId}/status")
    public ResponseEntity<PhoneVerificationStatusRes> status(@PathVariable UUID verificationId) {
        return ResponseEntity.ok(phoneVerificationService.getStatus(verificationId));
    }

    @Operation(summary = "Подтвердить кодом",
            description = "OTP из письма или тестовый код при app.telegram.allow-dev-confirm=true")
    @PostMapping("/{verificationId}/confirm-code")
    public ResponseEntity<PhoneVerificationStatusRes> confirmCode(
            @PathVariable UUID verificationId,
            @Valid @RequestBody PhoneVerificationConfirmCodeReq req) {
        return ResponseEntity.ok(phoneVerificationService.confirmWithDevCode(verificationId, req.code()));
    }
}
