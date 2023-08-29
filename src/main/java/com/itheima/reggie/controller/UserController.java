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

import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;


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

        @Autowired
        private RedisTemplate redisTemplate;

///发送手机验证码
//       @PostMapping("/sendMsg")
//        public R<String> sendMsg(@RequestBody User user, HttpSession session) {
//           //获取手机号
//           String phone = user.getPhone();
//           if (StringUtils.isNotEmpty(phone)) {
//               //随机生成的4位验证码
//               String code = ValidateCodeUtils.generateValidateCode(4).toString();
//               log.info("code={}", code);
//
////               //调用阿里云提供的短信服务发送信息
////               SMSUtils.sendMessage(phone, code);
////               //将生成的验证码保存到session
////               session.setAttribute(phone, code);
//               //将生成的验证码缓冲到redis中，并且设置有效期为5分钟
//               redisTemplate.opsForValue().set(phone,code,5,TimeUnit.MINUTES);
//
//               //调用阿里云提供的短信服务发送信息
//               SMSUtils.sendMessage(phone, code);
//               //将生成的验证码保存到session
//               session.setAttribute(phone, code);
//
//               return R.success("手机验证码短信发送成功 ");
//           }
//          return R.error("短信发送失败");
//       }
    /**
     * 发送邮箱验证码
     *
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        // 获取邮箱账号
        String phone = user.getPhone();

        String subject = "瑞吉餐购登录验证码";

        if (StringUtils.isNotEmpty(phone)) {
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            String context = "欢迎使用瑞吉餐购，登录验证码为: " + code + ",五分钟内有效，请妥善保管!";
            log.info("code={}", code);

            // 真正地发送邮箱验证码
            userService.sendMsg(phone, subject, context);

            //  将随机生成的验证码保存到session中
            //            session.setAttribute(phone, code);

            // 验证码由保存到session 优化为 缓存到Redis中，并且设置验证码的有效时间为 5分钟
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);

            return R.success("验证码发送成功，请及时查看!");
        }
        return R.error("验证码发送失败，请重新输入!");
    }


       @PostMapping("/login")
        public R<User> login(@RequestBody Map map, HttpSession session){
           log.info(map.toString());
           //获取手机号
           String phone = map.get("phone").toString();
           //获取验证码
           String code = map.get("code").toString();
           //从session中获取保存的验证码

           //Object codeInSession = session.getAttribute(phone);
           //从redis中获取验证码
           Object codeInRedis = redisTemplate.opsForValue().get(phone);

           //进行验证码的对比(页面提交的验证码和session中保存的验证码进行对比
           if(codeInRedis !=null&&codeInRedis.equals(code))

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

               //如果用户登录成功，删除Redis中缓存的验证码
               redisTemplate.delete(phone);
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
