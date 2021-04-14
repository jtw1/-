
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