package com.mobsandgeeks.saripaar.annotation;

import com.mobsandgeeks.saripaar.rule.SizeRule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ValidateUsing(SizeRule.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Size {
    int min() default -2147483648;

    int max() default 2147483647;

    boolean trim() default false;

    String filter() default "";

    int sequence() default -1;

    int messageResId() default -1;

    String message() default "Invalid length";

    int flags() default 0;

    int errorCode() default -1;
}