package com.serenmeet.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.serenmeet.auth.application.AdminAuthApplicationService;
import com.serenmeet.auth.dto.AdminUserView;
import com.serenmeet.common.ApiResponse;
import org.junit.jupiter.api.Test;

class AdminAuthControllerTest {

    @Test
    void returnsCurrentAuthenticatedAdminUser() {
        AdminUserView user = new AdminUserView(7L, "ops@serenmeet", "运营管理员");
        AdminAuthController controller = new AdminAuthController(mock(AdminAuthApplicationService.class));

        ApiResponse<AdminUserView> response = controller.me(user);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(user);
    }
}
