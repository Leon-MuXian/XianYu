package com.serenmeet.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  @Test
  void bindsIncomingRequestIdToResponseContextAndMdc() throws Exception {
    RequestIdFilter filter = new RequestIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdContext.HEADER_NAME, "request-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    CapturingFilterChain chain = new CapturingFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestIdContext.HEADER_NAME)).isEqualTo("request-123");
    assertThat(chain.responseRequestId).isEqualTo("request-123");
    assertThat(chain.contextRequestId).isEqualTo("request-123");
    assertThat(chain.mdcRequestId).isEqualTo("request-123");
    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  private static class CapturingFilterChain extends MockFilterChain {

    private String responseRequestId;
    private String contextRequestId;
    private String mdcRequestId;

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
      responseRequestId = ((HttpServletResponse) response).getHeader(RequestIdContext.HEADER_NAME);
      contextRequestId = RequestIdContext.get();
      mdcRequestId = MDC.get(RequestIdFilter.MDC_KEY);
    }
  }
}
