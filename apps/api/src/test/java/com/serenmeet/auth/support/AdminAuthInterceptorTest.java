package com.serenmeet.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.serenmeet.auth.application.AdminAuthApplicationService;
import com.serenmeet.auth.dto.AdminUserView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AdminAuthInterceptorTest {

  @Mock
  private AdminAuthApplicationService authService;

  @Test
  void storesAuthenticatedAdminUserOnRequest() {
    AdminUserView user = new AdminUserView(1L, "admin@serenmeet", "平台管理员");
    when(authService.requireUser("Bearer token")).thenReturn(user);
    when(authService.requireToken("Bearer token")).thenReturn("token");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");

    boolean result = new AdminAuthInterceptor(authService).preHandle(request, new MockHttpServletResponse(), new Object());

    assertThat(result).isTrue();
    assertThat(request.getAttribute(CurrentAdminUser.REQUEST_ATTRIBUTE)).isEqualTo(user);
    assertThat(request.getAttribute(CurrentAdminUser.TOKEN_ATTRIBUTE)).isEqualTo("token");
  }
}
