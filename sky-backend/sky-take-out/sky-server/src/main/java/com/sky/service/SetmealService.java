package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;

public interface SetmealService {
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     *
     * @param setmealDTO
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 批量删除套餐，在售状态下，不可删除套餐
     * @param ids
     */
    void delete(List<Long> ids);
}
