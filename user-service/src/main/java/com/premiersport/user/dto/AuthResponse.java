package com.premiersport.user.dto;

import com.premiersport.user.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserDto user;

    @Data
    @Builder
    public static class UserDto {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private List<UserEntity.Address> addresses;
        private String createdAt;
    }
}
