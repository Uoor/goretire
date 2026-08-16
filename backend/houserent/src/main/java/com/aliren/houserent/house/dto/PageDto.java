package com.aliren.houserent.house.dto;

import lombok.Data;

import java.util.List;

/** 分页响应：{ total, list } */
@Data
public class PageDto<T> {
    private long total;
    private List<T> list;

    public PageDto(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
}
