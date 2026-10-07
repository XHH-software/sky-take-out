package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    // 优化：校验收货地址是否超出配送范围
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

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

        // 校验用户的收货地址是否超出配送范围，最大允许100000米
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

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
        orderDetailMapper.inserBatch(orderDetails);

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

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getByOpenid(String.valueOf(userId));


        //调用微信支付接口，生成预支付交易单
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        JSONObject jsonObject = new JSONObject();
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @param pageNum 页面
     * @param pageSize 每页记录数
     * @param status 订单状态  1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    @Override
    public PageResult historyOrders(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status); // 设置订单状态
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId()); // 设置用户ID

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 定义集合 用于返回时PageResult的 当前页数据集合
        List<OrderVO> list = new ArrayList<>();

        // 判断查询到的结果不为空，总记录数大于0
        if (page != null && page.getTotal() > 0) {
            //添加 订单明细
            for (Orders orders : page) {
                Long orderId = orders.getId(); // 订单Id
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId); // 根据订单Id查询订单明细

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO); // 将 订单信息 拷贝到 VO中
                orderVO.setOrderDetailList(orderDetailList); // 设置 订单详情

                list.add(orderVO);
            }
        }
        // 返回 PageResult
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id 订单id
     */
    @Override
    public OrderVO details(Long id) {

        Orders orders = orderMapper.getById(id);

        // 根据订单Id查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id 订单id
     */
    @Override
    public void userCancelById(Long id) throws Exception {

        Orders ordersDB = orderMapper.getById(id);  // 根据订单id查询订单

        // 判断订单 不存在--抛异常 "订单不存在"
        if (ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 获取订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消；只有1和2可以直接取消订单
        Integer status = ordersDB.getStatus();
        if(status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR); // "订单状态错误"
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (status.equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口 不调用，注释
            /*weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
            */
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新 订单状态 6
        orders.setStatus(Orders.CANCELLED);
        // 更新 订单取消原因
        orders.setCancelReason("用户取消");
        // 更新 订单取消时间
        orders.setCancelTime(LocalDateTime.now());
        // 更新订单
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id 订单id
     */
    @Override
    public void repetition(Long id) {

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        /*List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            // 将原订单详情里面的菜品信息重新复制到购物车对象中，拷贝时跳过 `id` 字段
            BeanUtils.copyProperties(orderDetail, shoppingCart,"id");
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }*/

        // 将订单详情对象转换为购物车对象，使用stream流，简化 for 循环
        List<ShoppingCart> shoppingCartList = orderDetailList.stream()  // 1. 获取流
                .map(orderDetail -> {  // 2.中间操作：映射转换 OrderDetail → ShoppingCart
                    ShoppingCart shoppingCart = new ShoppingCart();
                    // 拷贝时跳过 `id` 字段
                    BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
                    shoppingCart.setUserId(BaseContext.getCurrentId());
                    shoppingCart.setCreateTime(LocalDateTime.now());
                    return shoppingCart;
                }).collect(Collectors.toList());  // 3.终止操作：收集成List

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索 - 管理端
     * @param ordersPageQueryDTO
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();

        // 空集合判断优化  if(ordersList != null && !ordersList.isEmpty())
        if(!CollectionUtils.isEmpty(ordersList)){
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                // StringBuilder 拼接
                StringBuilder sb = new StringBuilder();
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                for (OrderDetail s : orderDetailList) {
                    sb.append(s.getName()).append("*").append(s.getNumber()).append("；");
                }
                String orderDishes = sb.toString();
                /*
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                // Stream 直接拼接
                String orderDishes = orderDetailList.stream()
                        .map(s -> s.getName() + "*" + s.getNumber() + ";")
                        .collect(Collectors.joining());
                */

                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), orderVOList);
    }
    /*public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }
    /**
    // 根据订单id获取菜品信息字符串
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }*/

    /**
     * 各个状态的订单数量统计
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 接单
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder().id(ordersConfirmDTO.getId()).status(Orders.CONFIRMED).build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        /*Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }*/

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单
        // Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        //支付状态
        /*Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }*/

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 根据订单id更新订单状态、送达时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 根据订单id更新订单状态
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        log.info("开始校验配送范围，用户收货地址：{}，店铺地址：{}", address, shopAddress);
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        log.info("调用百度地图API，解析店铺地址：{}", shopAddress);
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        log.info("店铺地址解析返回结果：{}", shopCoordinate);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            log.error("店铺地址解析失败，status:{}", jsonObject.getString("status"));
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;
        log.info("店铺解析得到经纬度：{}", shopLngLat);

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        log.info("调用百度地图API，解析用户收货地址：{}", address);
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        log.info("用户地址解析返回结果：{}", userCoordinate);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            log.error("收货地址解析失败，status:{}", jsonObject.getString("status"));
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;
        log.info("用户收货地址解析得到经纬度：{}", userLngLat);

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        log.info("调用百度地图路线规划API，origin:{},destination:{}",shopLngLat,userLngLat);
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        log.info("路线规划接口返回：{}", json);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            log.error("配送路线规划失败，status:{}", jsonObject.getString("status"));
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");
        log.info("计算得到配送距离：{}米，最大允许100000米", distance);

        if(distance > 100000){
            //配送距离超过100000米
            log.warn("超出配送范围！距离{}米，限制100000米",distance);
            throw new OrderBusinessException("超出配送范围");
        }
        log.info("配送范围校验通过，距离{}米",distance);
    }
}