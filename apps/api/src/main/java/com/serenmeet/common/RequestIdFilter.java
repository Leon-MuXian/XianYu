package com.serenmeet.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个 HTTP 请求绑定同一个 requestId，并写入响应头和日志 MDC。
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String MDC_KEY = "requestId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    RequestIdContext.set(request.getHeader(RequestIdContext.HEADER_NAME));
    String requestId = RequestIdContext.get();
    MDC.put(MDC_KEY, requestId);
    response.setHeader(RequestIdContext.HEADER_NAME, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
      RequestIdContext.clear();
    }
  }
}
