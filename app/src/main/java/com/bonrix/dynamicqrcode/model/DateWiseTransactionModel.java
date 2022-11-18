package com.bonrix.dynamicqrcode.model;

public class DateWiseTransactionModel {
    private String Date;
    private String success;
    private String fail;
    private String pending;
    private String success_sum;
    private String fail_sum;
    private String pending_sum;

    public String getSuccess_sum() {
        return success_sum;
    }

    public void setSuccess_sum(String success_sum) {
        this.success_sum = success_sum;
    }

    public String getFail_sum() {
        return fail_sum;
    }

    public void setFail_sum(String fail_sum) {
        this.fail_sum = fail_sum;
    }

    public String getPending_sum() {
        return pending_sum;
    }

    public void setPending_sum(String pending_sum) {
        this.pending_sum = pending_sum;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getFail() {
        return fail;
    }

    public void setFail(String fail) {
        this.fail = fail;
    }

    public String getPending() {
        return pending;
    }

    public void setPending(String pending) {
        this.pending = pending;
    }
}
