package cn.chenxins.cms.service;


import cn.chenxins.cms.model.entity.LinLog;
import cn.chenxins.cms.model.entity.LinUser;
import cn.chenxins.cms.model.entity.mapper.LinUserMapper;
import cn.chenxins.cms.model.json.LogPageJsonOut;
import cn.chenxins.cms.model.json.UserJsonIn;
import cn.chenxins.cms.model.json.UserPageJsonOut;
import cn.chenxins.exception.BussinessErrorException;
import cn.chenxins.exception.ParamValueException;
import cn.chenxins.utils.DesUtils;
import cn.chenxins.utils.JdateUtils;
import cn.chenxins.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {


    @Autowired
    private LinUserMapper dbMapper;


    private LinUser getUserByName(String nickname) throws Exception {
        Example example = new Example(LinUser.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("deleteTime", null);
        criteria.andEqualTo("nickname", nickname.trim());
        return dbMapper.selectOneByExample(example);

    }

    private LinUser getUserByPhone(String phone) throws Exception {
        Example example = new Example(LinUser.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("deleteTime", null);
        criteria.andEqualTo("phone", phone);
        return dbMapper.selectOneByExample(example);

    }

    public LinUser getUserById(Integer uid) throws Exception {
        Example example = new Example(LinUser.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("deleteTime", null);
        criteria.andEqualTo("id", uid);
        return dbMapper.selectOneByExample(example);

    }

    public LinUser loginUser(UserJsonIn userJsonIn) throws Exception {
        LinUser user = getUserByPhone(userJsonIn.getPhone());
        if (user == null) {
            throw new BussinessErrorException("手机号码未注册");
        }
     /*   if (!DesUtils.CheckPasswordHash(user.getPassword(), userJsonIn.getPassword())) {
            throw new BussinessErrorException("用户名或密码错误");
        }*/
        return user;
    }

    public void register(LinUser userDTO) throws BussinessErrorException, Exception {
        if (StringUtils.isEmpty(userDTO.getCard())
                || StringUtils.isEmpty(userDTO.getNickname())
                || StringUtils.isEmpty(userDTO.getPhone())) {
            throw new ParamValueException("参数错误");
        }
        LinUser param = new LinUser();
        param.setPhone(userDTO.getPhone());
        LinUser linUserByPhone = dbMapper.selectOne(param);
        if (linUserByPhone != null) {
            throw new BussinessErrorException("手机号码已经存在");
        }

        LinUser cardParam = new LinUser();
        cardParam.setCard(userDTO.getCard());
        LinUser linUserByCard = dbMapper.selectOne(cardParam);
        if (linUserByCard != null) {
            throw new BussinessErrorException("身份证号码已经存在");
        }
        if (!StringUtils.isEmpty(userDTO.getCoachName())) {
            LinUser userByName = getUserByName(userDTO.getCoachName());
            userDTO.setCoach_id(userByName.getId());
        }

        userDTO.setCreateTime(JdateUtils.getCurrentDate());
        userDTO.setUpdateTime(JdateUtils.getCurrentDate());
        dbMapper.insert(userDTO);
    }

    public UserPageJsonOut getUsers(String nickname, Integer type, Integer subjectOne, Integer subjectTwo, String coachName, Integer page, Integer count,String start,String end) throws BussinessErrorException, Exception {
        // 开始分页
        PageHelper.startPage(page, count);
        Example example = new Example(LinUser.class);
        Example.Criteria criteria = example.createCriteria();

        if (StringUtil.isNotBlank(nickname)) {
            criteria.andLike("nickname", "%" + nickname + "%");
        }

        if (subjectOne != null) {
            criteria.andEqualTo("subjectOne", subjectOne);
        }
        if (subjectTwo != null) {
            criteria.andEqualTo("subjectTwo", subjectTwo);
        }

        if (type != null) {
            criteria.andEqualTo("type", type);
        }

        if (start != null && end != null) {
            criteria.andBetween("registerTime", start, end);
        }

        if (!StringUtils.isEmpty(coachName)){
            Example coachExample = new Example(LinUser.class);
            Example.Criteria criteriaExample = coachExample.createCriteria();
            criteriaExample.andEqualTo("nickname", coachName);
            criteriaExample.andEqualTo("type",2);
            List<LinUser> linUsers = dbMapper.selectByExample(coachExample);
            List<Integer> linUserIds = linUsers.stream().map(LinUser::getId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(linUserIds)){
                return new UserPageJsonOut(0,null);
            }
            criteria.andIn("coach_id",linUserIds);
        }

        example.orderBy("registerTime").desc();

        List<LinUser> alist = dbMapper.selectByExample(example);
        List<Integer> collect = alist.stream().map(LinUser::getCoach_id).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(collect)){
            Map<Integer, String> userIds = getUserIds(collect);
            alist.stream().forEach(
                    user->user.setCoachName(userIds.getOrDefault(user.getCoach_id(),""))
            );
        }

        int total = dbMapper.selectCountByExample(example);

        return new UserPageJsonOut(total,alist);
    }

    private Map<Integer,String> getUserIds(List<Integer> ids)  {
        Example example = new Example(LinUser.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andIn("id", ids);
        return dbMapper.selectByExample(example)
                .stream().collect(Collectors.toMap(LinUser::getId,LinUser::getNickname));
    }


    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateS(List<LinUser> userDTOs) throws BussinessErrorException, Exception {
        for (LinUser userDTO : userDTOs) {
            LinUser existUser = getUserById(userDTO.getId());
            if (existUser == null) {
                throw new BussinessErrorException("您要修改的用户信息不存在了");
            }
            if (!existUser.getPhone().equals(userDTO.getPhone())) {
                LinUser cardParam = new LinUser();
                cardParam.setPhone(userDTO.getPhone());
                LinUser linUserByPhone = dbMapper.selectOne(cardParam);
                if (linUserByPhone != null) {
                    throw new BussinessErrorException("手机号码已经存在");
                }
            }
            if (!existUser.getCard().equals(userDTO.getCard())) {
                LinUser cardParam = new LinUser();
                cardParam.setCard(userDTO.getCard());
                LinUser linUserByCard = dbMapper.selectOne(cardParam);
                if (linUserByCard != null) {
                    throw new BussinessErrorException("身份证号码已存在");
                }
            }
            BeanUtils.copyProperties(userDTO, existUser);
            existUser.setUpdateTime(JdateUtils.getCurrentDate());
            dbMapper.updateByPrimaryKey(existUser);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void updatePwd(Integer uid, UserJsonIn userJsonIn) throws BussinessErrorException, Exception {
/*        LinUser existUser = getUserById(uid);
        if (existUser == null) {
            throw new BussinessErrorException("您要修改密码的用户信息不存在了");
        }
        if (!DesUtils.CheckPasswordHash(existUser.getPassword(), userJsonIn.getOld_password())) {
            throw new BussinessErrorException("原始密码错误");
        }
        existUser.setPassword(DesUtils.GeneratePasswordHash(userJsonIn.getNew_password()));
        existUser.setUpdateTime(JdateUtils.getCurrentDate());
        dbMapper.updateByPrimaryKey(existUser);*/
    }

    public void delModelS(List<Integer> ids) {
        for (Integer id : ids) {
            LinUser linUser = dbMapper.selectOneByExample(id);
            if (linUser!=null)
            if (linUser.getDeleteTime()==null){
                linUser.setDeleteTime(JdateUtils.getCurrentDate());
                dbMapper.updateByPrimaryKey(linUser);
            }
        }
    }
}
