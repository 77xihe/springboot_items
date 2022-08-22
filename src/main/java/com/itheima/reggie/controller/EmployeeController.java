package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

  @PostMapping("/login")
  public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
      //密码进行md5加密
       String password = employee.getPassword();
       password =DigestUtils.md5DigestAsHex(password.getBytes());
      //根据用户名查数据库
      LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
      queryWrapper.eq(Employee::getUsername,employee.getUsername());
      Employee employee1 = employeeService.getOne(queryWrapper);
      //如果没有查询到则返回登录失败结果
      if(employee1==null){
          return R.error("登陆失败");
      }
      //密码对比 如果不一致则返回登录失败结果
      if(!employee1.getPassword().equals(password)){
          return R.error("登录失败");
      }
      //查看员工状态
      if(employee1.getStatus()==0)
      {
          return R.error("账号已禁用");
      }
      //登录成功，将员工id存入Session并返回登录成功结果
      request.getSession().setAttribute("employee",employee1.getId());
      log.info("用户已登录，用户为:{}",request.getSession().getAttribute("employee"));
      return R.success(employee1);
  }

  //员工退出
  @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
      //清理session中保存的当前员工的id
      request.getSession().removeAttribute("employee");
      return R.success("退出成功");
  }

  //新增员工
    @PostMapping
    public R<String> save( HttpServletRequest request ,@RequestBody  Employee employee){
      log.info("新增员工，员工信息 {}",employee.toString());
      //设置初始密码
      employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
      //设置创建时间  使用了mybatis-plus自动填充功能
//      employee.setCreateTime(LocalDateTime.now());
//      employee.setUpdateTime(LocalDateTime.now());
      //获得当前登录用户的id
        Long empId =(Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);

        employeeService.save(employee);
      return R.success("新增员工成功");
    }


    //员工信息分页查询
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        log.info("page={},pageSize={},name={}",page,pageSize,name);
        //构造分页构造器
        Page pageInfo=new Page(page,pageSize);
        // 构造条件构造器
        LambdaQueryWrapper <Employee>queryWrapper=new LambdaQueryWrapper();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotBlank(name),Employee::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo,queryWrapper);

      return R.success(pageInfo);
    }

    // 根据id修改员工信息
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee){
      log.info(employee.toString());

//      employee.setUpdateTime(LocalDateTime.now());
       Long empId =(Long) request.getSession().getAttribute("employee");
        employee.setUpdateUser(empId);
      employeeService.updateById(employee);
      return R.success("员工信息修改成功");
    }

    //编辑员工信息
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable  Long id){
      log.info("根据id来查询数据库");
        Employee employee = employeeService.getById(id);
        if(employee!=null)
        {
            return R.success(employee);
        }
        return  R.error("没有查询到对应员工信息");

    }

}



















