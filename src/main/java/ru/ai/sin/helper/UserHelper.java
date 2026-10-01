package ru.ai.sin.helper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.user.UserRepo;

@Service
@RequiredArgsConstructor
public class UserHelper implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) {
        UserEnt user = userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Failed to find user with username " + username));

        return  User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().getRole())
                .build();
    }
}
