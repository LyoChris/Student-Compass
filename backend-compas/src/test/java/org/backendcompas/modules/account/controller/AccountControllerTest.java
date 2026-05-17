package org.backendcompas.modules.account.controller;

import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.account.dto.UserProfileResponse;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountControllerTest {

    @Test
    void returnsCurrentUserProfile() {
        City city = new City();
        city.setId(UUID.randomUUID());
        city.setName("Iasi");
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setName("FII");
        faculty.setCity(city);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ana");
        user.setLastName("Popescu");
        user.setAge(20);
        user.setPhoneNumber("+40722123456");
        user.setEmail("ana@test.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);

        AccountController controller = new AccountController();
        UserProfileResponse response = controller.me(new CustomUserDetails(user));

        assertThat(response.email()).isEqualTo("ana@test.com");
        assertThat(response.cityName()).isEqualTo("Iasi");
        assertThat(response.facultyName()).isEqualTo("FII");
    }
}
