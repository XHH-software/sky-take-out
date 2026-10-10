package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单表
 */
@Mapper
public interface OrderMapper {
    /**
     * 向订单表插入1条数据
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询 -- 历史订单
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单Id查询订单
     * @param id 订单id
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据状态查询订单数量
     * @param status 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据状态和下单时间查询订单
     * @param status  订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @param orderTime 下单时间
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrdertimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 根据动态条件统计营业额
     * @param begin 开始时间
     * @param end 结束时间
     * @param status 订单状态
     */
    Double sumTurnover(Integer status, LocalDateTime begin, LocalDateTime end);

    /**
     * 根据动态条件统计营业额
     * @param begin 开始时间
     * @param end 结束时间
     * @param status 订单状态
     */
    Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status);

    /**
     * 查询商品销量排名
     * @param begin 开始时间
     * @param end 结束时间
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
