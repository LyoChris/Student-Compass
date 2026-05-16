package org.backendcompas.user.controller;

import org.backendcompas.security.auth.CustomUserDetails;
import org.backendcompas.user.dto.UserProfileResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return UserProfileResponse.from(currentUser.getUser());
    }
}
