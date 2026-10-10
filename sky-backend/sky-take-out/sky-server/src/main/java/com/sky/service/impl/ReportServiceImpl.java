package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    /**
     * 根据时间区间统计营业额
     * @param begin 开始日期
     * @param end 结束日期
     */
    @Override
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {
        // 将begin到end中的日期放入集合
        List<LocalDate> dateList = new ArrayList<>(); // 日期集合
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1); //日期计算，获得指定日期后1天的日期
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>(); // 营业额集合

        // 遍历日期集合，查询日期对应的营业额数据（订单已完成的金额合计）
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 获取当天开始的时分秒 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); // 获取当天结束的时分秒 23:59:59.999999999

            // select sum(amount) from orders where order_time > begin and order_time < end and status = status
            Double turnover = orderMapper.sumTurnover(Orders.COMPLETED,beginTime,endTime);

            // 如果当天营业额为空，那么turnover = 0.0
            if (turnover == null) turnover = 0.0;
            turnoverList.add(turnover);
        }

        //使用工具类将日期集合转为字符串，以逗号分隔
        String stringDateList = StringUtils.join(dateList, ",");
        //使用工具类将营业额集合转为字符串，以逗号分隔
        String stringTurnoverList = StringUtils.join(turnoverList, ",");

        return TurnoverReportVO.builder()
                               .dateList(stringDateList)
                               .turnoverList(stringTurnoverList)
                               .build();
    }
    /**
     * 根据时间区间统计用户数量
     * @param begin 开始日期
     * @param end 结束日期
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 将begin到end中的日期放入集合
        List<LocalDate> dateList = new ArrayList<>(); // 日期集合
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1); //日期计算，获得指定日期后1天的日期
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>(); // 新增用户数集合
        List<Integer> totalUserList = new ArrayList<>(); // 总用户数集合

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 获取当天开始的时分秒 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); // 获取当天结束的时分秒 23:59:59.999999999

            //查询新增用户数量 select count(id) from user where create_time > ? and create_time < ?
            Integer newUser = userMapper.getUserCount(beginTime, endTime);
            //查询总用户数量 select count(id) from user where  create_time < ?
            Integer totalUser = userMapper.getUserCount(null, endTime);

            if (newUser == null) newUser = 0;
            newUserList.add(newUser);

            if (totalUser == null) totalUser = 0;
            totalUserList.add(totalUser);
        }

        //使用工具类将 日期集合 转为字符串，以逗号分隔
        String stringDateList = StringUtils.join(dateList, ",");
        //使用工具类将 总用户数集合 转为字符串，以逗号分隔
        String stringTotalUserList = StringUtils.join(totalUserList, ",");
        //使用工具类将 新增用户数集合 转为字符串，以逗号分隔
        String stringNewUserList = StringUtils.join(newUserList, ",");

        return UserReportVO
                .builder()
                .dateList(stringDateList)
                .newUserList(stringNewUserList)
                .totalUserList(stringTotalUserList)
                .build();
    }
    /**
     * 根据时间区间统计订单数据
     * @param begin 开始日期
     * @param end 结束日期
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 将begin到end中的日期放入集合
        List<LocalDate> dateList = new ArrayList<>(); // 日期集合
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1); //日期计算，获得指定日期后1天的日期
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>(); //每天订单总数集合
        List<Integer> validOrderCountList = new ArrayList<>(); //每天有效订单数集合

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 获取当天开始的时分秒 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); // 获取当天结束的时分秒 23:59:59.999999999

            //查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = ?
            Integer validOrderCount = orderMapper.getOrderCount(beginTime, endTime, Orders.COMPLETED);

            //查询每天的总订单数 select count(id) from orders where order_time > ? and order_time < ?
            Integer orderCount = orderMapper.getOrderCount(beginTime, endTime, null);

            if (validOrderCount == null) validOrderCount = 0;
            validOrderCountList.add(validOrderCount);

            if (orderCount == null) orderCount = 0;
            orderCountList.add(orderCount);
        }
        //订单总数，使用stream流，reduce合并，相当于将集合遍历一遍进行累加和。get获得值
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //有效订单数 ==> 已完成的订单
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //订单完成率 = 有效订单数 / 总订单数
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0){ //判断分母不为0
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        //使用工具类将 日期集合 转为字符串，以逗号分隔
        String stringDateList = StringUtils.join(dateList, ",");
        //使用工具类将 总用户数集合 转为字符串，以逗号分隔
        String stringValidOrderCountList = StringUtils.join(validOrderCountList, ",");
        //使用工具类将 新增用户数集合 转为字符串，以逗号分隔
        String stringOrderCountList = StringUtils.join(orderCountList, ",");

        return OrderReportVO
                .builder()
                .dateList(stringDateList)
                .validOrderCountList(stringValidOrderCountList)
                .orderCountList(stringOrderCountList)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
    /**
     * 查询指定时间区间内的销量排名top10
     * @param begin 开始日期
     * @param end 结束日期
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN); // 获取当天开始的时分秒 00:00:00
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX); // 获取当天结束的时分秒 23:59:59.999999999

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime); // 查询商品销量排名

        //商品名称列表，以逗号分隔，例如：鱼香肉丝,宫保鸡丁,水煮鱼
        List<String> nameList = new ArrayList<>();
        for (GoodsSalesDTO dto : salesTop10) {
            nameList.add(dto.getName());
        }
        //等价 ==> List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());

        //销量列表，以逗号分隔，例如：260,215,200
        List<Integer> numberList = new ArrayList<>();
        for (GoodsSalesDTO dto : salesTop10) {
            numberList.add(dto.getNumber());
        }
        //等价 ==> List<String> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        //使用工具类将 商品名称列表 转为字符串，以逗号分隔
        String stringNameList = StringUtils.join(nameList, ",");
        //使用工具类将 销量列表 转为字符串，以逗号分隔
        String stringNumberList = StringUtils.join(numberList, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(stringNameList)
                .numberList(stringNumberList)
                .build();
    }
}
