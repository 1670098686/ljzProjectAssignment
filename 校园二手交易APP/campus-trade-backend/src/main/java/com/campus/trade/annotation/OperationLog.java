package com.campus.trade.annotation;

import com.campus.trade.model.enums.OperationType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    String title() default "";

    String action() default "";

    OperationType type() default OperationType.OTHER;

    String resourceId() default "";
}
