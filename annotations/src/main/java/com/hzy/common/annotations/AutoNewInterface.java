package com.hzy.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author huangyang
 * @Description: 在编译时，自动生成对应的接口
 * @date 2019/05/08 下午3:27
 *
 * 对类应用些注解，将自动生成包含该类public方法的接口
 *
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface AutoNewInterface {
    /**
     * 接口名
     * @return
     */
    String value() default "";

    /**
     * 接口所在包
     * @return
     */
    String packageName() default "" ;

}
