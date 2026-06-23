package com.phrolova.vitaelensbackend.exception;
//全局异常

import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//自动生成日志对象Logger log = LoggerFactory.getLogger(getClass());以直接使用log.info,log.error,log.warn,log.error,log.debug,log.trace
@Slf4j
//spring boot的全局异常处理器注解，统一处理项目中抛出的异常
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 业务异常
    // 注解表示该方法负责处理 BizException 类型的异常
    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        log.warn("业务异常： {}",e.getMessage());
        return Result.error(e.getErrorCode());
    }

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        // 从参数校验异常中取出第一条错误信息
        String message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    // 参数绑定异常
    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException e){
        String message = e.getFieldErrors().get(0).getDefaultMessage();
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    // 系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常",e);
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }
}
