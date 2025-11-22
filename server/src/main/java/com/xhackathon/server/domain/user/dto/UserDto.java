package com.xhackathon.server.domain.user.dto;

import com.xhackathon.server.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "사용자 정보")
public class UserDto {
    
    @Schema(description = "사용자 PID", example = "user_21")
    private String pid;
    
    @Schema(description = "사용자 표시 이름", example = "동민")
    private String displayName;
    
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;
    
    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.setPid(user.getPid());
        dto.setDisplayName(user.getDisplayName());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        return dto;
    }
}