package com.finance.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 用于标识需要记录操作日志的业务方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 操作类型
     */
    String value();

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否记录参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录结果
     */
    boolean recordResult() default true;

    /**
     * 业务对象类型（如：Transaction、Budget、Category等）
     */
    String businessType() default "";

    /**
     * 是否记录操作耗时
     */
    boolean recordExecutionTime() default true;
}