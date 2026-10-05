package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper; // 订单表
    @Autowired
    private OrderDetailMapper orderDetailMapper; // 订单明细表
    @Autowired
    private AddressBookMapper addressBookMapper; // 地址簿表
    @Autowired
    private ShoppingCartMapper shoppingCartMapper; // 购物车表
    /**
     * 用户下单
     * @param ordersSubmitDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理各种业务异常 "用户地址为空，不能下单"
        // Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 抛出业务异常 - 用户地址为空，不能下单
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 处理各种业务异常 "购物车数据为空，不能下单"
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId()); // Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectShoppingCartListByUserId(shoppingCart);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            // 抛出业务异常 - 购物车数据为空，不能下单
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(BaseContext.getCurrentId())+String.valueOf(System.currentTimeMillis())); //用户id+时间戳
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());
        orderMapper.insert(orders);

        // 准备一个集合 用于批量添加订单明细
        List<OrderDetail> orderDetails = new ArrayList<>();
        // 遍历购物车数据，拷贝到订单明细表中
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 设置当前订单明细关联的订单d
            orderDetails.add(orderDetail);
        }
        // 向订单明细表 批量 插入n条数据
        OrderDetailMapper.inserBatch(orderDetails);

        // 清空当前用户的购物车数据
        shoppingCartMapper.deleteById(BaseContext.getCurrentId());


        /*OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();
           return orderSubmitVO；
         */
        // 封账VO，返回
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();
    }
}
