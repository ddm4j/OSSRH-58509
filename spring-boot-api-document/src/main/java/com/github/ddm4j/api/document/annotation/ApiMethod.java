package com.github.ddm4j.api.document.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限制只能用于方法上，接口说明
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiMethod {

	/**
	 * 接口名称
	 * 
	 * @return
	 */
	public String value();

	/**
	 * 描述说明
	 * 
	 * @return
	 */
	public String describe() default "";

	/**
	 * 作者
	 * 
	 * @return
	 */
	public String author() default "";

	/**
	 * 版本，默认V1.0
	 * 
	 * @return
	 */
	public String version() default "V1.0";

	/**
	 * 设置不能要扫描的请求参数
	 * 
	 * @return
	 */
	public String[] hides() default {};
}
