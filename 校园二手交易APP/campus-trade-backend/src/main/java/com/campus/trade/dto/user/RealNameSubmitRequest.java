package com.campus.trade.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RealNameSubmitRequest {

    @NotBlank
    @Size(max = 50)
    private String realName;

    @NotBlank
    @Size(max = 30)
    private String idNumber;

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
}
