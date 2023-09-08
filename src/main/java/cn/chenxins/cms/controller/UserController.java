package cn.chenxins.cms.controller;

import cn.chenxins.authorization.annotation.AdminRequired;
import cn.chenxins.authorization.annotation.GroupRequired;
import cn.chenxins.authorization.annotation.LoggerReg;
import cn.chenxins.authorization.annotation.LoginRequired;
import cn.chenxins.authorization.annotation.RefreshTokenRequired;
import cn.chenxins.authorization.manager.TokenManager;
import cn.chenxins.cms.model.entity.LinUser;
import cn.chenxins.cms.model.json.TokenJsonOut;
import cn.chenxins.cms.model.json.UserJsonIn;
import cn.chenxins.cms.service.TicketService;
import cn.chenxins.cms.service.UserService;
import cn.chenxins.exception.BussinessErrorException;
import cn.chenxins.exception.ParamValueException;
import cn.chenxins.exception.TokenException;
import cn.chenxins.utils.ConstConfig;
import cn.chenxins.utils.ResultJson;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

import java.util.List;

@RestController
@EnableAutoConfiguration
@RequestMapping("drive/user")
@Api("tags=用户接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier(value = "memoryTokenManager")
    private TokenManager tokenManager;

    @Autowired
    private TicketService ticketService;


    @PostMapping("login")
    public Object userLogin(@RequestBody UserJsonIn userJsonIn) {

        /**
         * 校验入参
         */
        try {
            if (StringUtils.isEmpty(userJsonIn.getPhone())){
                throw new ParamValueException("手机号码不能为空");
            }
        } catch (ParamValueException pe) {
            return ResultJson.ParameterException(pe.getLocalizedMessage(), userJsonIn);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ParameterError();
        }

        try {
            LinUser linUser = userService.loginUser(userJsonIn);
            String uid = linUser.getId().toString();
            //创建token，并设进redis中
            String accessToken = tokenManager.createToken(uid);
            //设置refresh token进，并设进redis中
            String refreshToken = tokenManager.createReToken(accessToken);
            return new TokenJsonOut(accessToken, refreshToken, linUser);

        } catch (TokenException te) {
            return ResultJson.TokenRedisException();
        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @PostMapping("register")
    @AdminRequired
    @LoggerReg(template = "管理员新建了一个用户")
    public Object userRegister(@RequestBody LinUser userInfo,
                               @RequestHeader(value = "authorization",required = false) String authorization) {
        try {
            userService.register(userInfo);
            return ResultJson.Sucess();
        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @PutMapping("/")
    @LoginRequired
    public Object userUpdate(@RequestBody List<LinUser> users, NativeWebRequest webRequest,
                             @RequestHeader(value = "authorization",required = false) String authorization) {

        try {
            Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
            if (uid == null) {
                return ResultJson.ServerError();
            }
            userService.updateS(users);
            return ResultJson.Sucess();
        } catch (TokenException te) {
            return ResultJson.TokenRedisException();
        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }

    }



    @GetMapping(value = "search",name="查询用户")
    public ResultJson searchAllUserByKey(@RequestParam(required = false) String nickName
            ,@RequestParam(required = true) Integer type
            ,@RequestParam(required = false) Integer subjectOne
            ,@RequestParam(required = false) Integer subjectTwo
            ,@RequestParam(required = false) String coachName
            ,@RequestParam(required = false) Integer page
            ,@RequestParam(required = false) Integer count
    ,@RequestHeader(value = "authorization",required = false) String authorization){
        if (page==null) {
            page=1;
        }
        if (count==null) {
            count=10;
        }
        try {
            return ResultJson.Sucess(userService.getUsers(nickName, type, subjectOne, subjectTwo, coachName, page, count));
        }
        catch (BussinessErrorException be){
            return ResultJson.BussinessException(be.getLocalizedMessage());
        }
        catch (Exception e){
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @GetMapping(value = "/{userId}/{type}/tickets",name="ticket")
    @GroupRequired
    public ResultJson searchAllUserByKey(@PathVariable("userId") Integer userId
            , @PathVariable("type") Integer type
            , @RequestParam(required = false) Integer page
            , @RequestParam(required = false) Integer count
            , @RequestParam(required = false) String startDate
            , @RequestParam(required = false) String endDate,
                                         @RequestHeader(value = "authorization",required = false) String authorization) {

        if (page==null) {
            page=1;
        }
        if (count==null) {
            count=10;
        }

        try {
            return ResultJson.Sucess(ticketService.getUserTickets(userId, type, startDate, endDate, page, count));
        }
        catch (Exception e){
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }




    @PutMapping("change_password")
    @LoginRequired
    public Object changePwd(@RequestBody UserJsonIn userJsonIn, NativeWebRequest webRequest) {

        /**
         * 校验入参
         */

        try {
            Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
            if (uid == null) {
                return ResultJson.ServerError();
            }
            userService.updatePwd(uid, userJsonIn);
            return ResultJson.Sucess();
        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @GetMapping("refresh")
    @RefreshTokenRequired
    public Object refresh(NativeWebRequest webRequest,@RequestHeader(value = "authorization",required = false) String authorization) {
        try {
            Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
            if (uid == null) {
                return ResultJson.ServerError();
            }
            //创建token，并设进redis中
            String accessToken = tokenManager.createToken(uid.toString());

            LinUser user = userService.getUserById(uid);

            return new TokenJsonOut(accessToken, "", user);
        } catch (TokenException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @DeleteMapping(value = "",name="删除用户")
    @LoginRequired
    public Object deleteBook(@RequestBody List<Integer> ids,NativeWebRequest webRequest,@RequestHeader(value = "authorization",required = false) String authorization) {
        try {
            Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
            if (uid == null) {
                return ResultJson.ServerError();
            }
            if (ids.contains(uid)){
                throw new BussinessErrorException("不能删除自己");
            }
            userService.delModelS(ids);
            return ResultJson.Sucess();

        }catch (BussinessErrorException be){
            return ResultJson.BussinessException(be.getLocalizedMessage());
        }catch (Exception e){
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }
}
