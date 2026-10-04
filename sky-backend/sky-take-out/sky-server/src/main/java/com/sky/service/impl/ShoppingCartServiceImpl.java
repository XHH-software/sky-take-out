package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addshoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.selectShoppingCartListByUserId(shoppingCart);

        // 判断当前加入到购物车中的商品是否已经存在
        if(list !=null && !list.isEmpty()){

            // 取出第一条数据，因为查询后只有一条数据，所以也是唯一一条数据
            ShoppingCart cart = list.get(0);
            // 存在：数量+1，执行update语句
            cart.setNumber(cart.getNumber()+1);
            shoppingCartMapper.updateNumberById(cart);
        }else { // 不存在：执行insert语句，加入一条数据到购物车

            // 判断本次添加到购物车的是菜品还是套餐（判断 菜品Id 是否为空）
            if(shoppingCartDTO.getDishId() != null){
                //本次添加到购物车的是：菜品
                Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                //本次添加到购物车的是：套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.inset(shoppingCart);
        }
    }

    /**
     * 查看购物车
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        // 获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> list = shoppingCartMapper.selectShoppingCartListByUserId(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        // 获取到当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.cleanShoppingCartByUserId(userId);
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

        // 设置当前登录用户的用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.selectShoppingCartListByUserId(shoppingCart);

        // 判断当前加入到购物车中的商品是否已经存在
        if(list !=null && !list.isEmpty()) {

            // 取出第一条数据，因为查询后只有一条数据，所以也是唯一一条数据
            ShoppingCart cart = list.get(0);

            Integer number = cart.getNumber();
            // 判断数量是否为1
            if(number == 1){
                // 是：直接删除
                shoppingCartMapper.deleteById(cart.getId());
            }else{
                // 否：-1，执行update语句
                cart.setNumber(number-1);
                shoppingCartMapper.updateNumberById(cart);
            }
        }
    }
}
