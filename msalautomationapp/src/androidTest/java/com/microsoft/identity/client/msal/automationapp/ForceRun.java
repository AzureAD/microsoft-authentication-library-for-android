package com.microsoft.identity.client.msal.automationapp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(value = RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
public @interface ForceRun {
}
