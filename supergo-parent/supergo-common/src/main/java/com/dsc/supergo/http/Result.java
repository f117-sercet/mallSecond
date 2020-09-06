package com.dsc.supergo.http;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 结果类
 * @author dsc
 *
 */
@Getter
@Setter
public class Result implements Serializable {
    /**
     * 响应数据的状态
     响应数据的状态
     **/
    private boolean success;

    /**
     * 提示的信息
     */
    private String message;
    /**
     *  //记录所有错误信息
     */
    private List<Error> errors = new ArrayList<Error>();

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public Result(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Result() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
