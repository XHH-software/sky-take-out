package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 根据订单Id查询订单明细
     * @param orderId
     */
    @Select("SELECT * FROM  order_detail WHERE order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
