package ru.ai.sin.logic.analytics;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.analytics.dto.AnalyticsEventInReq;
import ru.ai.sin.logic.analytics.dto.AnalyticsFunnelDTO;
import ru.ai.sin.logic.analytics.dto.AnalyticsFunnelRow;
import ru.ai.sin.logic.analytics.dto.AnalyticsPathCountRow;
import ru.ai.sin.logic.analytics.dto.AnalyticsSummaryDTO;
import ru.ai.sin.logic.analytics.dto.AnalyticsSummaryReq;
import ru.ai.sin.logic.analytics.dto.EntityPopulationSummaryDTO;
import ru.ai.sin.logic.analytics.dto.EntityPopulationSummaryReq;
import ru.ai.sin.logic.registration.ClientIpResolver;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.tools.UserTools;
import ru.ai.sin.models.enums.AnalyticsEventType;
import ru.ai.sin.models.enums.RoleEnum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsEventRepo analyticsEventRepo;
    private final AnalyticsMinuteIpRateLimiter analyticsMinuteIpRateLimiter;
    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final RecruiterRepo recruiterRepo;
    private final UserTools userTools;

    @Override
    @Transactional
    public void recordEvent(AnalyticsEventInReq req, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        analyticsMinuteIpRateLimiter.check(ip);

        AnalyticsEventType type;
        try {
            type = AnalyticsEventType.fromCode(req.eventType());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        String path = req.path().trim();
        if (path.isEmpty()) {
            throw new BadRequestException("path must not be blank");
        }
        if (path.length() > 1024) {
            path = path.substring(0, 1024);
        }

        AnalyticsEventEnt e = new AnalyticsEventEnt();
        e.setEventType(type.getCode());
        e.setPath(path);
        e.setSessionId(req.sessionId());
        e.setUserAgent(trimTo(req.userAgent(), 512));
        e.setIpHash(sha256Hex(ip));
        userTools.findCurrentUserFetchingLinks().ifPresent(u -> e.setUserId(u.getId()));
        analyticsEventRepo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryDTO summarize(AnalyticsSummaryReq req) {
        if (!req.to().isAfter(req.from())) {
            throw new BadRequestException("Parameter 'to' must be after 'from'");
        }
        List<Object[]> rows = analyticsEventRepo.countByPathBetween(req.from(), req.to());
        List<AnalyticsPathCountRow> mapped = rows.stream()
                .map(r -> new AnalyticsPathCountRow((String) r[0], ((Number) r[1]).longValue()))
                .toList();
        return new AnalyticsSummaryDTO(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public EntityPopulationSummaryDTO summarizeEntityPopulation(EntityPopulationSummaryReq req) {
        LocalDateTime from = req.from();
        LocalDateTime to = req.to();
        if ((from == null) != (to == null)) {
            throw new BadRequestException("from and to must both be set or both omitted");
        }
        if (from != null && !to.isAfter(from)) {
            throw new BadRequestException("Parameter 'to' must be after 'from'");
        }

        Map<RoleEnum, Long> byRole = new EnumMap<>(RoleEnum.class);
        for (RoleEnum r : RoleEnum.values()) {
            byRole.put(r, 0L);
        }
        for (Object[] row : userRepo.countAllGroupedByRole()) {
            RoleEnum role = (RoleEnum) row[0];
            if (role != null) {
                byRole.put(role, ((Number) row[1]).longValue());
            }
        }

        long totalUsers = userRepo.count();
        long totalStudents = studentRepo.countAllStudents();
        long totalRecruiters = recruiterRepo.countAllRecruiters();

        Long newStudents = null;
        Long newRecruiters = null;
        if (from != null) {
            newStudents = studentRepo.countStudentsCreatedBetween(from, to);
            newRecruiters = recruiterRepo.countRecruitersCreatedBetween(from, to);
        }

        return new EntityPopulationSummaryDTO(
                totalUsers,
                byRole.getOrDefault(RoleEnum.RECRUITER, 0L),
                byRole.getOrDefault(RoleEnum.STUDENT, 0L),
                byRole.getOrDefault(RoleEnum.ADMIN, 0L),
                totalStudents,
                totalRecruiters,
                newStudents,
                newRecruiters);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsFunnelDTO summarizeFunnel(AnalyticsSummaryReq req) {
        if (!req.to().isAfter(req.from())) {
            throw new BadRequestException("Parameter 'to' must be after 'from'");
        }
        List<Object[]> rows = analyticsEventRepo.countByEventTypeBetween(req.from(), req.to());
        List<AnalyticsFunnelRow> mapped = rows.stream()
                .map(r -> new AnalyticsFunnelRow((String) r[0], ((Number) r[1]).longValue()))
                .toList();
        return new AnalyticsFunnelDTO(mapped);
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
