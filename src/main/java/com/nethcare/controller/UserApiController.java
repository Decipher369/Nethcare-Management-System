package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import com.nethcare.dto.UserDto;
import com.nethcare.dto.UserForm;
import com.nethcare.model.User;
import com.nethcare.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Staff account administration. Every path here is ADMIN-only in
 * SecurityConfig, so the check lives in the routing rules rather than in
 * each method.
 *
 * Responses go out as UserDto, not as the User entity. User does have a
 * getPasswordHash(), so returning it directly published every account's BCrypt
 * hash to anyone who could call GET /api/users.
 */
@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserDto>> list() {
        return ApiResponse.success(UserDto.of(userService.list()));
    }

    @PostMapping
    public ApiResponse<UserDto> create(@RequestBody UserForm form) {
        User created = userService.create(
                form.getUsername(), form.getPassword(), form.getFullName(),
                form.getEmail(), form.getRole());
        return ApiResponse.success("User created.", UserDto.of(created));
    }

    @PutMapping("/{id}/role")
    public ApiResponse<UserDto> changeRole(@PathVariable Long id, @RequestBody UserForm form) {
        return ApiResponse.success("Role updated.", UserDto.of(userService.changeRole(id, form.getRole())));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<UserDto> deactivate(@PathVariable Long id) {
        return ApiResponse.success("User deactivated.", UserDto.of(userService.deactivate(id)));
    }
}
