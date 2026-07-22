package com.serenmeet.auth.support;

public final class TenantContext {

  private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

  private TenantContext() {
  }

  public static void set(Long tenantId) {
    CURRENT.set(tenantId);
  }

  public static Long requireTenantId() {
    Long tenantId = CURRENT.get();
    if (tenantId == null) {
      throw new IllegalStateException("Tenant context is not available");
    }
    return tenantId;
  }

  public static Long get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
