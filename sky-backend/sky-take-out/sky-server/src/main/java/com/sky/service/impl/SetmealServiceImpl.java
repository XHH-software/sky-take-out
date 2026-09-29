package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
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

    /**
     * 批量删除套餐，在售状态下，不可删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        // 判断是否为在售状态
        ids.forEach(id ->{
            // 根据id查询套餐的在售状态
            Setmeal setmeal = setmealMapper.getById(id);

            if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
                // "起售中的套餐不能删除"
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        // 删除操作
        ids.forEach(id ->{
            // 删除套餐表中的数据
            setmealMapper.deleteById(id);
            // 删除套餐菜品关系表中的数据
            setmealDishMapper.deleteById(id);
        });
    }

    /**
     * 根据id查询套餐，用于修改页面回显数据
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // 根据套餐id查询套餐主表信息
        Setmeal setmeal = setmealMapper.getById(id);
        // 根据套餐id查询套餐关联的菜品集合（中间表setmeal_dish）
        List<SetmealDish> setmealDishes  = setmealDishMapper.getBySetmealId(id);

        // 创建VO对象，用于返回给前端
        SetmealVO setmealVO = new SetmealVO();
        // 属性拷贝：把setmeal实体的字段复制到VO
        BeanUtils.copyProperties(setmeal,setmealVO);
        // 将查询出来的套餐菜品集合存入VO，VO里才有这个List集合字段
        setmealVO.setSetmealDishes(setmealDishes);

        // 返回封装好的VO给Controller
        return setmealVO;
    }
}
