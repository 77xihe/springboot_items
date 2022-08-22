package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

//全局异常处理
@ControllerAdvice(annotations = {RestController.class})
@ResponseBody   //把结果封装成json数据 返回
@Slf4j
public class  GlobalExceptionHandler {
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());
        if(ex.getMessage().contains("Duplicate entry")) {
            String[] split = ex.getMessage().split(" ");
            String msg=split[2]+"用户名已存在";
           return  R.error(msg);

        }
        return R.error("未知错误");
    }

   @ExceptionHandler(CustomException.class)
    public R<String>  exceptionHandler(CustomException ex){

            log.error(ex.getMessage());
            return R.error(ex.getMessage());
        }
}
