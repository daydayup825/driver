package cn.chenxins.cms.controller;

import cn.chenxins.authorization.annotation.AdminRequired;
import cn.chenxins.authorization.annotation.GroupRequired;
import cn.chenxins.authorization.annotation.LoggerReg;
import cn.chenxins.authorization.annotation.LoginRequired;
import cn.chenxins.authorization.annotation.RefreshTokenRequired;
import cn.chenxins.authorization.manager.TokenManager;
import cn.chenxins.cms.model.dto.QueryParam;
import cn.chenxins.cms.model.entity.LinUser;
import cn.chenxins.cms.model.entity.mapper.LinUserDTO;
import cn.chenxins.cms.model.json.TokenJsonOut;
import cn.chenxins.cms.model.json.UserJsonIn;
import cn.chenxins.cms.model.json.UserPageJsonOut;
import cn.chenxins.cms.service.TicketService;
import cn.chenxins.cms.service.UserService;
import cn.chenxins.exception.BussinessErrorException;
import cn.chenxins.exception.ParamValueException;
import cn.chenxins.exception.TokenException;
import cn.chenxins.utils.CSVUtils;
import cn.chenxins.utils.ConstConfig;
import cn.chenxins.utils.CsvImportUtil;
import cn.chenxins.utils.JdateUtils;
import cn.chenxins.utils.ResultJson;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.NumberUtils;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Object userLogin(@RequestBody UserJsonIn userJsonIn) throws Exception {

        /**
         * 校验入参
         */
        try {
            if (StringUtils.isEmpty(userJsonIn.getNickname())) {
                throw new ParamValueException("身份证不能为空");
            }
        } catch (ParamValueException pe) {
            return ResultJson.ParameterException(pe.getLocalizedMessage(), userJsonIn);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ParameterError();
        }
        LinUser linUser = userService.loginUser(userJsonIn);
        if (linUser == null) {
            return new ResultJson(123, "身份证号未录入,请联系管理员录入", null);
        }
        try {

            String uid = linUser.getId().toString();
            //创建token，并设进redis中
            String accessToken = tokenManager.createToken(uid);
            //设置refresh token进，并设进redis中
            String refreshToken = tokenManager.createReToken(accessToken);
            return new TokenJsonOut(accessToken, refreshToken, linUser);

        } catch (TokenException te) {
            return ResultJson.TokenRedisException();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @PostMapping("logout")
    public Object userLogout(HttpServletRequest webRequest) {

        String bearerToken = getBearerToken(webRequest);

        tokenManager.deleteToken(bearerToken);
        return ResultJson.Sucess();
    }

    private String getBearerToken(HttpServletRequest request) {
        //从header中得到token
        String authss = request.getHeader(ConstConfig.AUTHORIZATION);
        if (authss == null) {
            return null;
        }
        String[] aa = authss.split("\\.");
        if (aa.length != 2)
            return null;
        // String baseToken=aa[1];
//        String ss= new String(Base64.getDecoder().decode(baseToken),"utf-8");
//        ss=ss.substring(0,ss.length()-1);
        return authss;
    }


    @PostMapping("register")
    @AdminRequired
    @LoggerReg(template = "管理员新建了一个用户")
    public Object userRegister(@RequestBody LinUser userInfo,
                               @RequestHeader(value = "authorization", required = false) String authorization) {
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

    @PostMapping("registers")
    @AdminRequired
    @LoggerReg(template = "管理员新建了一个用户")
    public Object userRegisters(@RequestBody List<LinUserDTO> userInfos,
                                @RequestHeader(value = "authorization", required = false) String authorization) {
        try {
            for (LinUserDTO userInfo : userInfos) {
                try {
                    LinUser linUser = new LinUser();
                    BeanUtils.copyProperties(userInfo, linUser);
                    if (userInfo.getSubjectTwoTimeLong() != null) {
                        linUser.setSubjectTwoTime(new Date(userInfo.getSubjectTwoTimeLong()));
                        userInfo.setSubjectTwoTimeLong(null);
                    }
                    if (userInfo.getSubjectThreeTimeLong() != null) {
                        linUser.setSubjectThreeTime(new Date(userInfo.getSubjectThreeTimeLong()));
                    }
                    if (userInfo.getRegisterTimeLong() != null) {
                        linUser.setRegisterTime(new Date(userInfo.getRegisterTimeLong()));
                    }
                    userService.register(linUser);

                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            return ResultJson.Sucess();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }


    @PutMapping()
    @LoginRequired
    public Object userUpdate(@RequestBody List<LinUser> users, NativeWebRequest webRequest,
                             @RequestHeader(value = "authorization", required = false) String authorization) {

        try {
            Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
            if (uid == null) {
                return ResultJson.ServerError();
            }
            System.out.println(users.size());
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


    @PostMapping(value = "search", name = "查询用户")
    @LoginRequired
    public ResultJson searchAllUserByKey(
            @RequestBody QueryParam queryParam
            , NativeWebRequest webRequest,
            @RequestHeader(value = "ticket", required = false) String ticket) throws Exception {
        if (queryParam.getPage() == null) {
            queryParam.setPage(1);
        }
        if (queryParam.getCount() == null) {
            queryParam.setCount(10);
        }
        Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
        if (uid == null) {
            return ResultJson.ServerError();
        }
        LinUser userById = userService.getUserById(uid);
        if (userById.getType() == 2) {
            queryParam.setCoachName(userById.getNickname());
        }
        try {
            return ResultJson.Sucess(userService.getUsers(queryParam.getCoachName(), queryParam.getSearchTimerangeType(), queryParam.getNickname(), queryParam.getType(), queryParam.getSubjectTwo(), queryParam.getSubjectThree(), queryParam.getIntroducer(), queryParam.getPage(), queryParam.getCount(), queryParam.getStartDate(), queryParam.getEndDate()));
        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }


 /*   @ApiOperation(value = "导出资产管理列表", notes = "导出资产管理列表", httpMethod = "POST")
    @PostMapping("/export/users")
    public void exportAssetsManageList(HttpServletResponse response, HttpServletRequest request,
                                       @RequestParam(required = false) String nickName
            ,@RequestParam(required = true) Integer type
            ,@RequestParam(required = false) Integer subjectOne
            ,@RequestParam(required = false) Integer subjectTwo
            ,@RequestParam(required = false) String coachName
            ,@RequestParam(required = false) Integer page
            ,@RequestParam(required = false) Integer count
            , @RequestParam(required = false) String startDate
            , @RequestParam(required = false) String endDate
            , @RequestParam(required = false) Integer searchTimerangeType
            ,@RequestHeader(value = "authorization",required = false)String authorization) throws Exception {

        String sTitle = "nickname,card,phone,cost,coachName,subjectTwo,subjectThree,registerTimeStr,introducer";
        String fName = "UsersManage_";
        String mapKey = "nickname,card,phone,cost,coachName,subjectTwo,subjectThree,registerTimeStr,introducer";

        if (page==null) {
            page=1;
        }
        if (count==null) {
            count=1000000;
        }
        List<Map> users = userService.getUsers(searchTimerangeType,nickName, type, subjectOne, subjectTwo, coachName, page, count, startDate, endDate).getCollection()
                .stream().map(user ->
                {
                    String dateGenFormat = JdateUtils.getDateGenFormat(user.getRegisterTime());
                    user.setRegisterTimeStr(dateGenFormat);
                    return JSON.parseObject(JSON.toJSONString(user), Map.class);
                })
                .collect(Collectors.toList());
        try (final OutputStream os = response.getOutputStream()) {
            CSVUtils.responseSetProperties(request, fName, response);
            CSVUtils.doExport(users, sTitle, mapKey, os);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


    /**
     * 导出资产管理列表"
     */
    @ApiOperation(value = "导出资产管理列表", notes = "导出资产管理列表", httpMethod = "POST")
    @PostMapping("/export/users/id")
    public ResultJson users(HttpServletResponse response, HttpServletRequest request,
                            @RequestBody List<Integer> ids
    ) throws Exception {

        String sTitle = "名称,身份证号,金额,教练,科目二,科目三,注册时间,介绍人";
        String mapKey = "nickname,card,cost,coachName,subjectTwo,subjectThree,registerTimeStr,introducer";

        List<Map> users = userService.getUsersByIds(ids)
                .stream().map(user ->
                {
                    return JSON.parseObject(JSON.toJSONString(user), Map.class);
                })
                .collect(Collectors.toList());
        String s = CSVUtils.doExport(users, sTitle, mapKey);
        return ResultJson.Sucess(s);
    }

    @GetMapping(value = "/csvData/{ts}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCSVData(
            @PathVariable(value = "ts") String ts) throws IOException {
        Resource resource = new FileSystemResource("/data/path/" + ts + ".xls");
        File file = resource.getFile();
        String csvContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
        return csvContent;
    }


    @ApiOperation(value = "导入资产管理", notes = "导入资产管理", httpMethod = "POST")
    @PostMapping(value = "/import/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void csvImportAssetsManage(@RequestPart("file") MultipartFile file) {
        // 使用CSV工具类，生成file文件
        File csvFile = CsvImportUtil.uploadFile(file);
        // 将文件内容解析，存入List容器，List<String>为每一行内容的集合，6为CSV文件每行的总列数
        List<List<String>> lists = CsvImportUtil.readCSV(csvFile.getPath(), 9);


        lists.stream().forEach(info -> {
            // 处理业务逻辑代码
            try {
                System.out.println("info:" + info);
                LinUser user = new LinUser();
                user.setNickname(info.get(0));
                user.setCard(info.get(1));
                user.setPhone(info.get(2));
                if (!StringUtils.isEmpty(info.get(3))) {
                    user.setCost(NumberUtils.parseNumber(info.get(3), BigDecimal.class));
                }
                user.setCoachName(info.get(4));
                if (!StringUtils.isEmpty(info.get(5))) {
                    user.setSubjectTwo(NumberUtils.parseNumber(info.get(5), Integer.class));
                }
                if (!StringUtils.isEmpty(info.get(6))) {
                    user.setSubjectThree(NumberUtils.parseNumber(info.get(6), Integer.class));
                }
                if (!StringUtils.isEmpty(info.get(7))) {
                    //    user.setRegisterTime(new Date(info.get(7)));
                }
                user.setIntroducer(info.get(8));
                try {
                    if (!StringUtils.isEmpty(info.get(9))) {
                        user.setType(NumberUtils.parseNumber(info.get(9), Integer.class));
                    }
                } catch (Exception e) {
                    user.setType(3);
                }

                userService.register(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }


    @PostMapping(value = "/{userId}/{type}/tickets", name = "ticket")
    @GroupRequired
    public ResultJson searchAllUserByKey(@PathVariable("userId") Integer userId
            , @PathVariable("type") Integer type
            , @RequestBody
                                         QueryParam queryParam,
                                         @RequestHeader(value = "ticket", required = false) String ticket) {

        if (queryParam.getPage() == null) {
            queryParam.setPage(1);
        }
        if (queryParam.getCount() == null) {
            queryParam.setCount(100);
        }

        try {
            return ResultJson.Sucess(ticketService.getUserTickets(userId, type, queryParam.getStartDate(), queryParam.getEndDate(), queryParam.getPage(), queryParam.getCount()));
        } catch (Exception e) {
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
    public Object refresh(NativeWebRequest webRequest, @RequestHeader(value = "authorization", required = false) String authorization) {
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

    @DeleteMapping(value = "", name = "删除用户")
    @LoginRequired
    public Object deleteBook(@RequestBody List<Integer> ids, NativeWebRequest webRequest, @RequestHeader(value = "token", required = false) String token) {
        try {
            Integer uid = (Integer) webRequest.getAttribute(ConstConfig.CURRENT_USER_ID, RequestAttributes.SCOPE_REQUEST);
            if (uid == null) {
                return ResultJson.ServerError();
            }
            if (ids.contains(uid)) {
                throw new BussinessErrorException("不能删除自己");
            }
            userService.delModelS(ids);
            return ResultJson.Sucess();

        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }
}
