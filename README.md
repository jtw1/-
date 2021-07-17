
# 电商秒杀
## 技术栈
spring boot+Mybatis

### 异常处理
1. 为 handlerException() 方法继续添加 @ResponseBody 注解即可将返回的 object 返回给前端页面。
            该方式会将异常的所有栈信息序列化后输出到前端页面，因此还需要继续处理，只将前端需要的异常信息返回给前端。
2. 将 ex 强转为 BusinessException ，然后使用其 getErrCode、getErrMsg 方法获取前端需要的异常信息，将其封装为 Map 后封装到 CommonReturnType 对象中，然后再返回给前端。
3. 优化：使用 CommonReturnType 的静态方法 create 构造对象并返回。
4. 继续完善该方法。
   判断 exception 是否为 BusinessException 类型，如果不是则为 CommonReturnType 对象赋值 errCode 为 EmBusinessError 枚举中的 UNKNOWN_ERROR 的 code 和 msg。
5. 继续优化异常处理。
      因为该处理方式是所有 controller 都需要的方式，因此将其抽象为 BaseController 中的方法，然后让其他 controller 组件去继承该 controller。
6. 总结：
   1. 定义一个 CommonReturnType, 能够用对应的 status, object 的方式返回所有的被 JSON 序列化对象，供前端解析使用，摒弃掉了使用 httpstatuscode + 内线 tomcat 自带的 error 页面方式去处理响应数据以及异常。
   2. 并且定义了一个 BusinessException ，统一管理我们自定义的错误码。
   3. 然后，在 BaseController 中定义一个所有 Controller 的基类，使用其中注解了 @ExceptionHandler 的方法来处理所有被 Controller 层捕获的异常。
      按照异常的种类由2种处理方式，一种是自定义 BusinessException, 其中有自定义的 error 的 code 和 msg，一种是未知的异常，采用统一处理方式。
   4. 异常处理方法上还可以添加日志相关组件，方便项目运行记录与错误排查。
7. Enum中int类型的状态码，如果以0开头，则会在controller组件中返回到前端时，如果使用了JSON序列化，解析时会省略数字前面的0，因此不应该以0开头

> 3.6 用户获取 otp 短信验证码
1. 需要按照一定的规则生产OTP 验证码
2. 将 OTP 验证码通对应用户的手机号关联（一般使用Redis处理，此处采用 session 模仿实现）使用spring注入方式注入一个HttpServletRequest对象。该对象其实是通过 spring bean 包装的 request 对象，本质是 proxy 模式（spring 在注入 HttpServletRequest 时，发现如果注入的是 一个 ObjectFactory 类型的对象时，就会将注入的 bean 替换成一个 JDK 动态代理对象，代理对象在执行 HttpServletRequest 对象里的方法时，就会通过 RequestObjectFactory.getObject() 获取一个 新的 request 对象来执行。），即多例模式?。
Spring能实现在多线程环境下，将各个线程的request进行隔离，且准确无误的进行注入，奥秘就是ThreadLocal. 它的内部拥有 ThreadLocal 方式的 map，去让用户在每个线程中处理自己对应的 request 中的数据，并且有ThreadLocal清除的机制。
3. 将 OTP 验证码通过短信通道发送给用户
> 解决跨域问题

添加注解  
`@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")` 
并且需要配合前端设置 xhrFields授信后使得跨域session共享
前端Ajax请求设置   `xhrFields:{withCredentials:true}`
> 使用手机号注册用户时，如何避免同一手机号多次注册

在数据库中给用户表的手机号字段添加唯一索引  点击对应table->右键更改table->index，然后index name随意，type为unique，然后选中手机号字段，storage type选择为BTREE 即可

跨域感知session
跨域感知session需要解决两个问题，第一个是解决跨域问题，第二个是解决跨域cookie传输问题

跨域问题
解决跨域问题有很多种方式，可以参考本章最后的扩展资料跨域问题的解决方式，我们在一开始的课程视频中使用了springboot自带的crossOrigin注解，如下（注意，和目前的课程中不完全一致，如何演进的继续往下看）

@CrossOrigin(origins = {"*"},allowedHeaders = "*")
这个注解一加后，所有的http response头上都会加上
Access-Control-Allow-Origin * 以及
Access-Control-Allow-Headers * 两个头部，这样可以满足CORS的跨域定义，我们的ajax看到这两个头部就认定对应的域名接收任何来自或不来自于本域的请求

跨域传递cookie的问题
跨域和跨域传递cookie是两个不同纬度的问题，我们依靠上述的方式解决了跨域的问题，但是要做到跨域感知session需要解决在跨域的前提下将cookie也能传上去，这个时候就需要设置另外一个头部 ，我们的cross origin演变为

`@CrossOrigin(origins = {"*"},allowCredentials = "true",allowedHeaders = "*")`
使用了allowCredentials后Access-Control-Allow-Credentials头被设置成true，同时前端设置xhrField:{withCredential:true}后，浏览器在ajax请求内带上对应的cookie头部和后端的allowCredentials配合在一起解决跨域传递cookie的问题。由于课程中仅仅使用了get和post的方法，而这两个方法在跨域请求中都是可以用的，因此allowedHeaders可以不加。

另外当设置了allowCredentials = “true"的时候origins = {”*"}就失效了，因为一旦设置了跨域传递cookie就不能再设置接受任何origins了，而springboot的实现方式是返回的allow origin取request内的origin即我们自己对应的html页面的路径。这样就可以做到在哪个origin上使用跨域就允许哪个origin，一样能达到我们想要的效果。

ps：许多浏览器包括safari和最新版本的chrome默认设置都是不支持携带跨域cookie的，即便我们代码写成允许，浏览器底层也做了限制，因此在调试的时候我们可以关闭对应的限制，也可以使用扩展阅读内的其他跨域处理方式

> 分布式会话管理

1. 基于cookie传输sessionID：java tomcat容器内嵌session实现 迁移到redis    (详见工程代码  controller/UserController.java  的login方法中)

2. 基于token传输类似sessionID：Java代码session实现迁移到redis。token比cookie更为安全，且存储容量更大

   ```java
   @Autowired
   //先引入redisTemplate
   private RedisTemplate redisTemplate;  
   
   // 生成登陆凭证token，UUID
   String uuidToken = UUID.randomUUID().toString();
   uuidToken=uuidToken.replace("-","");
   //建立token和用户登录态之间的联系
   redisTemplate.opsForValue().set(uuidToken,userModel);
   //设置过期时间
   redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);
   //最后下发token
   return CommonReturnType.create(uuidToken);
   ```

   前端页面对应的更改

   ```javascript
   success:function(data){
        if (data.status == "success") {
            alert("登陆成功");
            // 引入登录的token后，需添加以下部分
            var token=data.data;
    		window.localStorage["token"]=token;                     window.location.href="listItem.html";
         }
   ```

   OrderController.java的createOrder方法中，登录验证为

   ```java
   String token = httpServletRequest.getParameterMap().get("token")[0];
   if(StringUtils.isEmpty(token)){
               throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
           }
           //获取用户的登陆信息
   UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
   if(userModel == null){
     throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
   }
   ```

   