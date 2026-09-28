package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import com.sky.result.PageResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 插入数据
     * @param category
     */
    @Insert("INSERT INTO category(type, name, sort, status, create_time, update_time, create_user, update_user)  " +
            "VALUES (#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(OperationType.INSERT)//自定义注解
    void insert(Category category);

    /**
     * 启用、禁用分类,根据主键动态修改属性
     * @param category
     */
    @AutoFill(OperationType.UPDATE)
    void update(Category category);

    /**
     * 根据类型查询分类，修改分类，数据回显
     * @param type
     * @return
     */
    List<Category> list(Integer type);

    /**
     * 根据id删除分类
     * @param id
     */
    @Select("DELETE FROM category WHERE id = #{id}")
    void deleteById(Long id);
}
