package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
    @RestController
    @RequestMapping("/user")
    public class UserController {
        @Autowired
        private UserService userService;

       @PostMapping("/sendMsg")
        public R<String> sendMsg(@RequestBody User user, HttpSession session) {
           //获取手机号
           String phone = user.getPhone();
           if (StringUtils.isNotEmpty(phone)) {
               //随机生成的4位验证码
               String code = ValidateCodeUtils.generateValidateCode(4).toString();
               log.info("code={}", code);

               //调用阿里云提供的短信服务发送信息
               SMSUtils.sendMessage(phone, code);
               //将生成的验证码保存到session
               session.setAttribute(phone, code);
               return R.success("手机验证码短信发送成功 ");
           }
          return R.error("短信发送失败");
       }

       @PostMapping("/login")
        public R<User> login(@RequestBody Map map, HttpSession session){
           log.info(map.toString());
           //获取手机号
           String phone = map.get("phone").toString();
           //获取验证码
           String code = map.get("code").toString();
           //从session中获取保存的验证码
           Object codeInSession = session.getAttribute(phone);

           //进行验证码的对比(页面提交的验证码和session中保存的验证码进行对比
           if(codeInSession!=null&&codeInSession.equals(code))
           {
               //如果能够对比成功 说明登录成功
               //判断当前手机号对应的用户是否为新用户 如果是新用户就自动完成注册
               LambdaQueryWrapper <User>queryWrapper=new LambdaQueryWrapper();
               queryWrapper.eq(User::getPhone,phone);
               User user = userService.getOne(queryWrapper);
               if(user==null){
                   user=new User();
                   user.setPhone(phone);
                   user.setStatus(1);
                   userService.save(user);
               }
               session.setAttribute("user",user.getId());
              return R.success(user);

           }

           return R.error("登陆失败");

       }
    /**
     * 退出功能
     * ①在controller中创建对应的处理方法来接受前端的请求，请求方式为post；
     * ②清理session中的用户id
     * ③返回结果（前端页面会进行跳转到登录页面）
     * @return
     */
    @PostMapping("/loginout")
    public R<String> logout(HttpServletRequest request){
        //清理session中的用户id
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }
}
