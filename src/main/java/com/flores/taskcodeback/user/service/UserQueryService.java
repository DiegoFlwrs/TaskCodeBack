package com.flores.taskcodeback.user.service;

import com.flores.taskcodeback.user.dto.UserDto;

import java.util.List;

public interface UserQueryService {

    List<UserDto> getTeamMembers(String email);
}
