package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/*
* 套餐管理
*
* */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    @CacheEvict(value = "setmealCache",allEntries = true)
   @PostMapping
 public R<String> save(@RequestBody SetmealDto setmealDto){
     setmealService.savewithDish(setmealDto);
     return R.success("新增套餐成功！！");
 }

 //套餐分页查询
 @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
     //分页构造器
     Page<Setmeal> pageInfo=new Page<>(page ,pageSize);
     Page<SetmealDto> dtoPage=new Page<>(page,pageSize);

     LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
     //添加查询条件 模糊查询
     queryWrapper.like(name!=null,Setmeal::getName,name);
     //排序条件
     queryWrapper.orderByDesc(Setmeal::getUpdateTime);
     setmealService.page(pageInfo,queryWrapper);

     //将查询出来的page拷贝到dtopage
     BeanUtils.copyProperties(pageInfo,dtoPage,"records");
     List<Setmeal> records = pageInfo.getRecords();
     List<SetmealDto>list= records.stream().map((item)->{
         SetmealDto setmealDto=new SetmealDto();
         BeanUtils.copyProperties(item,setmealDto);
         Long categoryId = item.getCategoryId();
         Category category = categoryService.getById(categoryId);
         if(category!=null){
             String categoryName = category.getName();
             setmealDto.setCategoryName(categoryName);
         }
          return setmealDto;
     }).collect(Collectors.toList());

     dtoPage.setRecords(list);
     return R.success(dtoPage) ;
 }



//http://localhost:8080/setmeal?ids=1557375148643782658
    //allEntries表示删除某一分类下的所有数据
 @CacheEvict(value = "setmealCache",allEntries = true)
 @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){

     setmealService.removeWithDish(ids);
     return R.success("套餐删除成功！！");
 }


    @PostMapping("/status/{status}")
    public R<String> update1(@PathVariable int status,Long [] ids){
        for (Long id : ids) {
            Setmeal setmeal=new Setmeal();
            setmeal.setId(id);
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }
     return R.success("修改套餐状态成功");
    }



    //前台客户端展示  根据条件查询套餐数据

    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId+'_'+#setmeal.status")
    //前台客户端展示  根据条件查询套餐数据
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
     LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper();
     queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
     queryWrapper.eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
     queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);


    }

}


