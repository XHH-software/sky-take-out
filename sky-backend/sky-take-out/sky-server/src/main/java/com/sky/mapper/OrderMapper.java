package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单表
 */
@Mapper
public interface OrderMapper {
    /**
     * 向订单表插入1条数据
     */
    void insert(Orders orders);
}
