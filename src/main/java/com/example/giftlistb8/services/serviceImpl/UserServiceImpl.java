package com.example.giftlistb8.services.serviceImpl;


import com.example.giftlistb8.config.JwtService;
import com.example.giftlistb8.dto.PaginationResponse;
import com.example.giftlistb8.dto.SimpleResponse;
import com.example.giftlistb8.dto.user.requests.UpdateBlockStatus;
import com.example.giftlistb8.dto.user.response.UserResponseGetAll;
import com.example.giftlistb8.dto.user.response.UserResponseGetById;
import com.example.giftlistb8.entities.User;
import com.example.giftlistb8.entities.UserInfo;
import com.example.giftlistb8.repositories.*;
import com.example.giftlistb8.repositories.custom.CustomUserRepository;
import com.example.giftlistb8.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final CustomUserRepository customUserRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final JwtService jwtService;


    @Override
    public PaginationResponse<UserResponseGetAll> getAllUsers(int size, int page) {
        log.info("Getting all users");
        return customUserRepository.getAllUsers(size, page);
    }

    @Override
    public UserResponseGetById getById(Long userId) {
        Long currentUserId = jwtService.getUserInToken().getId();
        UserResponseGetById userResponseGetById = customUserRepository.getById(userId);
        userResponseGetById.setInFriends(userRepository.inMyFriends(currentUserId,userId));
        userResponseGetById.setInRequests(userRepository.inMyRequests(currentUserId,userId));
        log.info("Getting user by id: {}", userId);
        return userResponseGetById;
    }


    @Transactional
    @Override
    public SimpleResponse deleteById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        UserInfo userInfo = user.getUserInfo();
        if (userInfo != null) {
            userInfoRepository.delete(userInfo);

        } return customUserRepository.deleteById(userId);
    }


    @Override
    public SimpleResponse updateBlockedStatus(UpdateBlockStatus updateBlockStatus) {
        log.info("Updating blocked status of user with id: {}", updateBlockStatus.userId());
        Optional<User> userOptional = userRepository.getUserById(updateBlockStatus.userId());
        if (!userOptional.isPresent()) {
            return SimpleResponse.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(String.format("User with %s id not found", updateBlockStatus.userId()))
                    .build();
        }
        User user = userOptional.get();
        user.setIsBlocked(true);
        userRepository.save(user);
        log.info("Successfully updated blocked status of user with id: {}", updateBlockStatus.userId());

        return SimpleResponse.builder()
                .status(HttpStatus.OK)
                .message(String.format("User with id %s successfully blocked", updateBlockStatus.userId())).build();
    }
}


