package com.example.giftlistb8.services.serviceImpl;

import com.example.giftlistb8.config.JwtService;
import com.example.giftlistb8.dto.auth.requests.AuthAuthenticateRequest;
import com.example.giftlistb8.dto.auth.requests.AuthRegisterRequest;
import com.example.giftlistb8.dto.auth.responses.AuthRegisterResponse;
import com.example.giftlistb8.entities.User;
import com.example.giftlistb8.enums.Role;
import com.example.giftlistb8.exceptions.AlreadyExistsException;
import com.example.giftlistb8.exceptions.BadRequestException;
import com.example.giftlistb8.exceptions.NotFoundException;
import com.example.giftlistb8.repositories.UserRepository;
import com.example.giftlistb8.services.AuthService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    @Override
    public AuthRegisterResponse register(AuthRegisterRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.email())) {
            throw new AlreadyExistsException("Sorry, this email is already registered. Please try a different email or login to your existing account");
        }
        var user = User.builder()
                .firstName(userRequest.firstName())
                .lastName(userRequest.lastName())
                .email(userRequest.email())
                .password(passwordEncoder.encode(userRequest.password()))
                .isBlocked(false)
                .role(Role.USER)
                .build();
        userRepository.save(user);
        String jwtToken = jwtService.generateToken(user);

        return AuthRegisterResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(jwtToken)
                .build();
    }


    @Override
    public AuthRegisterResponse authenticate(AuthAuthenticateRequest userRegisterRequest) {
        User user = userRepository.findByEmail(userRegisterRequest.email())
                .orElseThrow(() ->
                        new BadRequestException("Invalid email or password."));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userRegisterRequest.email(),
                            userRegisterRequest.password()
                    )
            );
        } catch (AuthenticationException e) {
            log.error("Invalid email or password");
            throw new BadCredentialsException("Invalid email or password.");
        }
        String token = jwtService.generateToken(user);

        return AuthRegisterResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .build();
    }

    @PostConstruct
    void init() {
        try {
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new ClassPathResource("giftlist-b8.json").getInputStream());
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .build();
            log.info("Successfully works the init method");
            FirebaseApp firebaseApp = FirebaseApp.initializeApp(firebaseOptions);
        } catch (IOException e) {
            log.error("IOException");
        }
    }

    @Override
    public AuthRegisterResponse authWithGoogle(String tokenId) throws FirebaseAuthException {
        FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(tokenId);
        if (!userRepository.existsByEmail(firebaseToken.getEmail())) {
            User newUser = new User();
            String[] name = firebaseToken.getName().split(" ");
            newUser.setFirstName(name[0]);
            newUser.setLastName(name[1]);
            newUser.setEmail(firebaseToken.getEmail());
            newUser.setPassword(firebaseToken.getEmail());
            newUser.setRole(Role.USER);
            userRepository.save(newUser);
        }
        User user = userRepository.findByEmail(firebaseToken.getEmail()).orElseThrow(() -> {
            log.error(String.format("User with this %s email not found!", firebaseToken.getEmail()));
            throw new NotFoundException(String.format("User with this %s email not found!!", firebaseToken.getEmail()));
        });
        String token = jwtService.generateToken(user);
        log.info("Successfully works the authorization with google method");
        return AuthRegisterResponse.builder()
                .email(firebaseToken.getEmail() + " token: " + token)
                .build();
    }

}
