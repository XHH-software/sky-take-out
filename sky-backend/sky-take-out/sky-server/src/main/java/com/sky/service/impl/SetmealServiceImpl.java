package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 1.开启分页：PageHelper
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 2.执行mapper查询，返回Page<SetmealVO>，里面封装了总条数、列表
        Page<SetmealVO> setmealList = setmealMapper.page(setmealPageQueryDTO);
        // 3.封装PageResult返回给前端
        return new PageResult(setmealList.getTotal(), setmealList.getResult());
    }

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 创建套餐实体类对象，对应数据库setmeal表
        Setmeal setmeal = new Setmeal();

        // 属性拷贝：将前端传来的DTO中的套餐基础属性复制到setmeal实体
        BeanUtils.copyProperties(setmealDTO,setmeal);

        // 1.向套餐表添加数据
        setmealMapper.insert(setmeal);

        // 获取数据库自增生成的套餐主键id（依靠MyBatis useGeneratedKeys主键回填）
        Long setmealId = setmeal.getId();

        // 2.向套餐菜品表添加0~n条数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes(); // 得到DTO中的套餐菜品列表

        if (setmealDishes != null && !setmealDishes.isEmpty()) { // 判断套餐菜品列表是否为空
            setmealDishes.forEach(s -> { // 遍历套餐菜品列表
                s.setSetmealId(setmealId); // 给每一条套餐菜品设置套餐id，建立和套餐主表的关联关系
            });

            // 批量插入套餐菜品数据到setmeal_dish中间表
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }
}
