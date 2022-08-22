package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//菜品和套餐分类管理
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
//新增分类
@PostMapping
 public R<String> save(@RequestBody Category category){
    log.info("category:{}",category);
   categoryService.save(category);
   return R.success("新增分类成功");
 }

 //分页查询
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize){
    //分页构造器
    Page <Category> pageInfo=new Page<>(page,pageSize);
    //构造条件构造器
        LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();
        //根据sort排序
        queryWrapper.orderByAsc(Category::getSort);
        //进行分页查询
        categoryService.page(pageInfo,queryWrapper);
    return R.success(pageInfo);

    }

    //删除分类
    @DeleteMapping
    public R<String> delete(Long ids){
         log.info("删除分类，id为:{}",ids);
        //categoryService.removeById(ids);
        categoryService.remove(ids);

        return R.success("删除成功");
    }

    ///修改分类
    @PutMapping
    public R<String> update(@RequestBody Category category){
      log.info("修改分类信息{}",category);
      categoryService.updateById(category);
       return R.success("修改分类信息成功");
    }

  //根据条件查询分类
  @GetMapping("/list")
  public R<List<Category>> list(Category category){
    LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();
    queryWrapper.eq(category.getType()!=null,Category::getType,category.getType());
    queryWrapper.orderByAsc(Category::getSort)   //ORDER BY sort ASC
                .orderByDesc(Category::getUpdateTime);
      List<Category> list = categoryService.list(queryWrapper);   //list就是selec *

      return R.success(list);

  }

}
