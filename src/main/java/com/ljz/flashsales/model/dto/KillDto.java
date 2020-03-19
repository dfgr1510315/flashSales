package com.ljz.flashsales.model.dto;

import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ToString
public class KillDto implements Serializable {

    private Integer userId;

    @NotNull
    private Integer killId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getKillId() {
        return killId;
    }

    public void setKillId(Integer killId) {
        this.killId = killId;
    }
}
