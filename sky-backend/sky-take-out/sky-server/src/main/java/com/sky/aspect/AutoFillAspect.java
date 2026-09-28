package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面（切入点+通知）AutoFillAspect，统一拦截加入了@AutoFill的方法。
 * 通过反射为共享字段赋值
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    // 任意返回值  com.sky.mapper 包下所有类  类中任意方法  任意数量、任意类型参数
    public void autoFillPointCut() {
    }

    /**
     * 前置通知，在通知中进行公共字段的赋值
     */
    @Before("autoFillPointCut()")
    public void autoFillBefore(JoinPoint joinPoint) {

        log.info("开始进行公共字段自动填充...”");

        //获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); // 获取连接点签名，强转为`MethodSignature`才能拿到 Method 对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); // 反射获取方法上`@AutoFill`注解
        OperationType operationType = autoFill.value(); // 获取数据库操作类型，拿到枚举 `INSERT` / `UPDATE`，区分新增还是修改

        //获取到当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs(); // 获取目标 Mapper 方法的所有参数
        if(args == null || args.length == 0){
            return;
        }
        Object entity = args[0]; // 约定第一个参数就是实体类

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据当前不同的操作类型，为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT){
            // 插入操作，为4个公共字段赋值
            try {
                // .getClass().getDeclaredMethod(方法名, 参数类型);
                // getDeclaredMethod：获取当前类所有权限方法（public/private），不包含父类；
                // getMethod：只能获取public方法（包括父类 public）
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射为对象赋值
                // .invoke(对象, 参数值); 执行 setter 方法，给实体对象属性赋值
                setCreateUser.invoke(entity, currentId);
                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(operationType == OperationType.UPDATE){
            // 修改操作，为2个公共字段赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,  LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                // 通过反射为对象赋值
                setUpdateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
