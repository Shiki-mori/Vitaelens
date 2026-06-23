package com.phrolova.vitaelensbackend.exception;
//业务异常

import com.phrolova.vitaelensbackend.common.ErrorCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException{
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode){
        // 将 errorCode 的 message 赋给父类 Throwable(Throwable cause) 的内部属性 detailMessage
        // 调用链：RuntimeException(String)-> Exception(String)-> Throwable(String)
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }
}
