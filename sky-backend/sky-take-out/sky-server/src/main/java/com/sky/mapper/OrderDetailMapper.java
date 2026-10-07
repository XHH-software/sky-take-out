package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 订单明细表
 */
@Mapper
public interface OrderDetailMapper {
    /**
     * 向订单明细表 批量 插入n条数据
     * @param orderDetails
     */
    void inserBatch(List<OrderDetail> orderDetails);
}
