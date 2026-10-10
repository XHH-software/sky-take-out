package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    /**
     * 营业额数据统计
     * @param begin 开始日期
     * @param end 结束日期
     */
    TurnoverReportVO getTurnover(LocalDate begin, LocalDate end);
    /**
     * 用户数据统计
     * @param begin 开始日期
     * @param end 结束日期
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);
    /**
     * 订单数据统计
     * @param begin 开始日期
     * @param end 结束日期
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);
    /**
     * 销量排名统计
     * @param begin 开始日期
     * @param end 结束日期
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 导出近30天的运营数据报表
     */
    void exportBusinessData(HttpServletResponse response);
}
