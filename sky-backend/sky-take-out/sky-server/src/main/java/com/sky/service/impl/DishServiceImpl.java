package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO,dish);

        // 向菜品表添加1条数据
        dishMapper.insert(dish);

        // 向口味表添加0~n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){

            flavors.forEach(f->{
                f.setDishId(dish.getId()); //主键回显，获取insert语句生成的主键值
            });

            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 删除菜品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 起售中的菜品不能删除,被套餐关联的菜品不能删除,删除菜品后，关联的口味数据也需要删除掉
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id); // 根据主键查询菜品，.getStatus()获取起售状态
            // 判断是否是起售中的菜品？是--不能删除。 1：起售中。
            if(StatusConstant.ENABLE.equals(dish.getStatus())){
                // "起售中的菜品不能删除"
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 根据菜品id查询对应的套餐id
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);

        // 判断是否是被套餐关联的菜品？是--不能删除。
        if (setmealIds != null && !setmealIds.isEmpty()){
            //  "当前菜品关联了套餐,不能删除"
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        /*for (Long id : ids) {
            //删除菜品表中的菜品数据
            dishMapper.deleteById(id);

            //删除口味表中的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }*/
        // 批量删除菜品表中的菜品数据
        // DELETE FROM dish WHERE id IN (?,?,?)
        dishMapper.deleteByIds(ids);

        // 批量删除口味表中的口味数据
        // DELETE FROM dish_flavor WHERE dish_id IN (?,?,?)
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //将查询到的数据封装到dishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        // 菜品基本信息 直接更新
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        // 口味数据 先删除，再插入
        // 根据菜品ID删除原有的口味数据（删除旧口味）
        dishFlavorMapper.deleteByDishId(dish.getId());

        // 插入口味数据
        List<DishFlavor> flavorList = dishDTO.getFlavors(); // 得到口味数据集合
        if(flavorList != null && !flavorList.isEmpty()){
            // 设置每条菜品的ID
            flavorList.forEach(f->{
                f.setDishId(dishDTO.getId());// 主键回显，获取insert语句生成的主键值
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavorList);
        }
    }

    /**
     * 菜品起售停售(根据ID修改菜品dish表的status）菜品停售，则包含菜品的套餐同时停售。
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        // 构建菜品对象，封装要修改的字段：id(定位菜品) + status(起售/停售状态)
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        dishMapper.update(dish);

        // 只有菜品【停售】才处理套餐
        if (status == StatusConstant.DISABLE) {
            // 找到菜品对应的套餐ID，装菜品id到集合，因为Mapper方法参数接收List集合,根据套餐ID设置套餐停售
            ArrayList<Long> idList = new ArrayList<>();
            idList.add(id);

            // 根据菜品id，查询setmeal_dish关联表，获取所有包含该菜品的套餐id
            List<Long> setmealDishIds = setmealDishMapper.getSetmealIdsByDishIds(idList);
            log.info("包含要停售的菜品套餐id:{}",setmealDishIds);

            // 查询出来的套餐id集合不为null且不为空（存在关联套餐）
            if (setmealDishIds != null && !setmealDishIds.isEmpty()){

                // 遍历所有关联套餐id，逐个把套餐修改为停售
                for (Long setmealDishId : setmealDishIds) {
                    log.info("菜品套餐:{}",setmealDishId);
                    // 构建套餐对象：套餐id + 停售状态
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealDishId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }
}