package ru.ai.sin.logic.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.config.property.UserProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataInitializer implements ApplicationRunner {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    private final UserProperties userProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        userProperties.getLogins().forEach(
                user -> createIfNotExists(
                        user.getUsername(),
                        user.getPassword(),
                        RoleEnum.fromRole(user.getRole())
                ));
    }

    private void createIfNotExists(
            String username,
            String rawPassword,
            RoleEnum role
    ) {

        if (userRepo.existsByUsername(username)) return;

        UserEnt user = userRepo.save(new UserEnt(role, username, passwordEncoder.encode(rawPassword)));

        log.info("User {} created", user.getId());
    }
}
