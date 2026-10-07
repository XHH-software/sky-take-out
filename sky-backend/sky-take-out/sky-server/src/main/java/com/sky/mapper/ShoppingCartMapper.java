package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 购物车表
 */
@Mapper
public interface ShoppingCartMapper {

    /**
     * 根据用户ID查询商品
     * @param shoppingCart
     */
    List<ShoppingCart> selectShoppingCartListByUserId(ShoppingCart shoppingCart);

    /**
     * 根据Id修改数量
     * @param shoppingCart
     */
    @Update("UPDATE shopping_cart SET number = #{number} WHERE id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 插入一条数据到购物车
     * @param shoppingCart
     */
    @Insert("INSERT INTO shopping_cart(name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time)" +
            "VALUES (#{name}, #{image}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void inset(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     * @param userId
     */
    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void cleanShoppingCartByUserId(Long userId);

    /**
     * 根据Id删除购物车记录
     * @param id
     */
    @Delete("DELETE FROM shopping_cart WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 批量插入购物车数据
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
