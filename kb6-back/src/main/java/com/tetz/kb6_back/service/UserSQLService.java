package com.tetz.kb6_back.service;

import com.tetz.kb6_back.dto.UserDto;
import com.tetz.kb6_back.entity.User;
import com.tetz.kb6_back.repository.UserSQLRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSQLService {
    private final UserSQLRepository userSQLRepository;

    public void saveUser(UserDto userDto) {
        User user = convertToEntity(userDto);
        userSQLRepository.save(user);
    }

    public UserDto getUser(String userId) {
        Optional<User> userOptional = userSQLRepository.findById(userId);
        return userOptional.map(this::convertToDto).orElse(null);
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userSQLRepository.findAll();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public void deleteUser(String userId) {
        userSQLRepository.deleteById(userId);
    }

    public void updateUser(UserDto userDto) {
        if (userSQLRepository.existsById(userDto.getId())) {
            User user = convertToEntity(userDto);
            userSQLRepository.save(user);
        }
    }

    // Entity와 DTO 간 변환 메서드
    private User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setAge(userDto.getAge());
        return user;
    }

    private UserDto convertToDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getAge());
    }
}
