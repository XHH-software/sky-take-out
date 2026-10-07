package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     * @param pageNum 页面
     * @param pageSize 每页记录数
     * @param status 订单状态  1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    PageResult historyOrders(int pageNum, int pageSize, Integer status);

    /**
     * 查询订单详情
     * @param id 订单id
     */
    OrderVO details(Long id);

    /**
     * 取消订单
     * @param id 订单id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再来一单
     * @param id 订单id
     */
    void repetition(Long id);

    /**
     * 订单搜索 - 管理端
     * @param ordersPageQueryDTO
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 取消订单
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 完成订单
     */
    void complete(Long id);

    /**
     * 派送订单
     */
    void delivery(Long id);
}
