package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CategoryService categoryService;




    @PostMapping
    public R<String> save(@RequestBody DishDto dishdto){
        log.info(dishdto.toString());

        dishService.saveFlavor(dishdto);


        //清理某个分类下面的菜品
        String key="dish_"+dishdto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }




    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //添加排序条件 根据更新时间 降序排
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //已有的查询 不能查到菜品名称
        //对象拷贝  需要修改records 所以不拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);


        //清理所有菜品缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        //清理某个分类下面的菜品
        String key="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("修改菜品成功");
    }


    //修改菜品状态
    @PostMapping("/status/{status}")
    public R<String> update2(@PathVariable  int status,long [] ids) {
        for (long id : ids) {
            Dish dish = new Dish();
            dish.setId(id);
            dish.setStatus(status);
            dishService.updateById(dish);


        }
        return R.success("修改菜品状态成功");
    }

    //删除菜品http://localhost:8080/dish?ids=1556825412555763713
    // http://localhost:8080/dish?ids=1556226009444843521,1413384757047271425
    @DeleteMapping
    public R<String> delete2(long [] ids){
        for (long id : ids) {
            //删除菜品表
            dishService.removeById(id);
            //删除口味表
            LambdaQueryWrapper <DishFlavor>queryWrapper=new LambdaQueryWrapper();
            queryWrapper.eq(DishFlavor::getDishId,id);
            dishFlavorService.remove(queryWrapper);
        }
       return R.success("批量删除成功!");
    }
//根据条件查询菜品数据
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//         LambdaQueryWrapper <Dish>queryWrapper=new LambdaQueryWrapper();
//         queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
//         queryWrapper.eq(Dish::getStatus,1);  //查询状态为1
//         queryWrapper.orderByAsc(Dish::getSort)
//                     .orderByDesc(Dish::getUpdateTime);  //ORDER BY sort ASC,update_time DESC
//        List<Dish> list = dishService.list(queryWrapper);
//
//        return R.success(list);
//    }

    //移动端获取列表以后 还要获取口味信息 所以要把dish改成dishDto
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){

        List<DishDto> dishDtoList=null;
        String key="dish"+dish.getCategoryId()+"_"+dish.getStatus();
        //先从redis中获取缓存数据
        dishDtoList =(List<DishDto>) redisTemplate.opsForValue().get(key);
        if(dishDtoList!=null){
            //如果存在 直接返回 无需查询数据库
            return R.success(dishDtoList);
        }
        LambdaQueryWrapper <Dish>queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);  //查询状态为1
        queryWrapper.orderByAsc(Dish::getSort)
                .orderByDesc(Dish::getUpdateTime);  //ORDER BY sort ASC,update_time DESC
        List<Dish> list = dishService.list(queryWrapper);

         dishDtoList= list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //根据菜品id获取到口味信息
            Long id = item.getId(); // 当前菜品的id
            LambdaQueryWrapper <DishFlavor>queryWrapper1=new LambdaQueryWrapper();
            queryWrapper1.eq(DishFlavor::getDishId,id);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(queryWrapper1);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());


        //如果不存在 需要查询数据库  将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key,dishDtoList,1l, TimeUnit.HOURS);


        return R.success(dishDtoList);
    }

}

















