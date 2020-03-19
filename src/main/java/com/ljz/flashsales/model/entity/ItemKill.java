package com.ljz.flashsales.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

public class ItemKill implements Serializable {
    private Integer id;

    private Integer item_id;

    private Integer total;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date start_time;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date end_time;

    private Byte isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date create_time;

    private String itemName;

    //采用服务器时间控制是否可以进行抢购
    private Integer canKill;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getItem_id() {
        return item_id;
    }

    public void setItem_id(Integer item_id) {
        this.item_id = item_id;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Date getStart_time() {
        return start_time;
    }

    public void setStart_time(Date start_time) {
        this.start_time = start_time;
    }

    public Date getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Date end_time) {
        this.end_time = end_time;
    }

    public Byte getIsActive() {
        return isActive;
    }

    public void setIsActive(Byte isActive) {
        this.isActive = isActive;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getCanKill() {
        return canKill;
    }

    public void setCanKill(Integer canKill) {
        this.canKill = canKill;
    }

    @Override
    public String toString() {
        return "ItemKill{" +
                "id=" + id +
                ", item_id=" + item_id +
                ", total=" + total +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", isActive=" + isActive +
                ", create_time=" + create_time +
                ", itemName='" + itemName + '\'' +
                ", canKill=" + canKill +
                '}';
    }
}