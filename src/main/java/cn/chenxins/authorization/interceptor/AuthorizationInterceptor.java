package cn.chenxins.authorization.interceptor;

import cn.chenxins.authorization.annotation.AdminRequired;
import cn.chenxins.authorization.annotation.LoggerReg;
import cn.chenxins.authorization.annotation.LoginRequired;
import cn.chenxins.authorization.annotation.RefreshTokenRequired;
import cn.chenxins.authorization.manager.TokenManager;
import cn.chenxins.cms.model.entity.LinLog;
import cn.chenxins.cms.model.entity.LinUser;
import cn.chenxins.cms.service.UserService;
import cn.chenxins.utils.ConstConfig;
import cn.chenxins.utils.JdateUtils;
import cn.chenxins.utils.JsonUtils;
import cn.chenxins.utils.MetaJson;
import cn.chenxins.utils.ResultJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;

@Component
public class AuthorizationInterceptor   extends HandlerInterceptorAdapter {

    @Autowired
    @Qualifier(value = "memoryTokenManager")
    private TokenManager manager;

    @Autowired
    private UserService userService;

    @Autowired
    WebApplicationContext applicationContext;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        System.out.println("afterCompletion");
        //如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        LoggerReg loggerReg=method.getAnnotation(LoggerReg.class);
        if (loggerReg != null && loggerReg.template()!=null) {
        //根据方法声明的注解走相应的验证流程
            try {
                String template=loggerReg.template();
                LinUser user=getCurrentUser(request);
                if (user==null)
                {
                    user=new LinUser();
                    user.setId(999999);
                    user.setNickname("匿名用户");
                }
                String msg=template.replaceAll("\\{user.nickname\\}",user.getNickname());
                LinLog log=new LinLog();
                log.setMessage(msg);
                log.setTime(JdateUtils.getCurrentDate());
                log.setUserId(user.getId());
                log.setUserName(user.getNickname());
                log.setMethod(request.getMethod());
                log.setPath(request.getRequestURI());
                log.setStatusCode(response.getStatus());
                Map<String, MetaJson> authMap=MetaJson.getMetaMapUsingUri(applicationContext);
                if (authMap!=null){
                    MetaJson metaJson=authMap.get(request.getRequestURI());
                    if (metaJson!=null &&metaJson.getAuth()!=null)
                    {
                        log.setAuthority(metaJson.getAuth());
                    }
                }

                System.out.println("Logger reg finish:"+log.toString());

            }catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) {
        //如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        //根据方法声明的注解走相应的验证流程
        try {
            if (method.getAnnotation(LoginRequired.class)!=null)
            {
                //从header中得到token
                String tokenkey=getBearerToken(request);
                System.out.println(tokenkey);
                if (tokenkey == null || "".equals(tokenkey.trim()))
                {
                    return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN,"无法正常获取TOKEN");
                }
                String checkMsg=loginRequired(tokenkey,request);
                System.out.println(checkMsg);
                if ("OK".equals(checkMsg))
                {
                    return true;
                } else {
                    return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN,checkMsg);
                }
            }
            else if (method.getAnnotation(AdminRequired.class) !=null)
            {
                String tokenkey=getBearerToken(request);
                if (tokenkey==null || "".equals(tokenkey.trim()))
                {
                    return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN,"无法正常获取TOKEN");
                }
                String checkMsg=adminRequired(tokenkey,request);
                if ("OK".equals(checkMsg)){
                    return true;
                } else {
                    return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN,checkMsg);
                }

            }

            else if (method.getAnnotation(RefreshTokenRequired.class) !=null) {
                String tokenkey = getBearerToken(request);
                if (tokenkey == null  || "".equals(tokenkey.trim())) {
                    return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN, "无法正常获取TOKEN");
                }
                Integer uid=manager.refreshCheckToken(tokenkey);
                if (uid==null)
                {
                    return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN, "令牌过期了");
                }
                //如果token验证成功，将token对应的用户id存在request中，便于之后注入
                request.setAttribute(ConstConfig.CURRENT_USER_ID, uid);
                return true;
            }
            else {
                return true;   // 不需要拦截，直接通过
            }


        }catch (Exception e) {
            e.printStackTrace();
            return returnResponseMsg(response, HttpServletResponse.SC_FORBIDDEN,"处理授权出现异常");
        }
    }

    private LinUser getCurrentUser(HttpServletRequest request) throws Exception{
        LinUser user=(LinUser)request.getAttribute(ConstConfig.CURRENT_USER_TOKEN);
        if (user==null){
            Integer uid=(Integer)request.getAttribute(ConstConfig.CURRENT_USER_ID);
            if (uid!=null) {
                user=userService.getUserById(uid);
            }
        }
        return user;
    }

    private String loginRequired(String keyToken,HttpServletRequest request){
        try {
            //验证token,,只验存在redis里面。
            Integer uid = manager.checkToken(keyToken);
            if (uid == null) {
                return "权限过期请重新登录";
            }
            //如果token验证成功，将token对应的用户id存在request中，便于之后注入
            request.setAttribute(ConstConfig.CURRENT_USER_ID, uid);
            return "OK";
        }catch (Exception e){
            e.printStackTrace();
            return "出现异常，请联系管理员";
        }
    }


    private String adminRequired(String keyToken, HttpServletRequest request) {
        try {
            //验证token,,只验存在redis里面。
            Integer uid = manager.checkToken(keyToken);
            if (uid == null) {
                return "权限过期请重新登录";
            } else {
                LinUser curUser = userService.getUserById(uid);
                if (curUser != null) {
                    //如果token验证成功，将token对应的用户对象存在request中，便于之后注入
                    request.setAttribute(ConstConfig.CURRENT_USER_TOKEN, curUser);
                    return "OK";
                } else {
                    return "只有超级管理员可操作";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "出现异常，请联系管理员";
        }
    }


    private String getBasicAuthToken(String authss) throws Exception{
        if (authss==null)
        {
            return null;
        }
        String[] aa=authss.split("\\ ");
        if (aa.length!=2)
            return null;
        String baseToken=aa[1];
        String ss= new String(Base64.getDecoder().decode(baseToken),"utf-8");
        ss=ss.substring(0,ss.length()-1);
        return ss;
    }
    private String getBearerToken(HttpServletRequest request) throws Exception{
        //从header中得到token
        String authss = request.getHeader(ConstConfig.AUTHORIZATION);
        if (authss==null)
        {
            return null;
        }
        String[] aa=authss.split("\\.");
        if (aa.length!=2){
            return null;
        }
        return authss;
    }

    private boolean returnResponseMsg(HttpServletResponse response, int code, String msg) {
        try {
            response.setStatus(code);
            ResultJson outJson= ResultJson.Forbidden(msg);

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));

            writer.write(JsonUtils.objectToJson(outJson));
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            return false;
        }
    }
}
