package com.serenmeet.auth.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentSession {
  String REQUEST_ATTRIBUTE = CurrentSession.class.getName() + ".principal";
  String TOKEN_ATTRIBUTE = CurrentSession.class.getName() + ".token";
}
