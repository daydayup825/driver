package cn.chenxins.cms.service;


import cn.chenxins.cms.model.entity.LinUser;
import cn.chenxins.cms.model.entity.Ticket;
import cn.chenxins.cms.model.entity.mapper.LinUserMapper;
import cn.chenxins.cms.model.entity.mapper.TicketMapper;
import cn.chenxins.cms.model.json.TicketPageJsonOut;
import cn.chenxins.exception.BussinessErrorException;
import cn.chenxins.utils.JdateUtils;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.common.condition.UpdateByConditionMapper;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TicketService {


    @Autowired
    private TicketMapper ticketMapper;


    @Autowired
    private LinUserMapper linUserMapper;


    public TicketPageJsonOut getUserTickets(Integer userId, Integer type, String startDate, String endDate, Integer page, Integer count) {
        PageHelper.startPage(page, count);
        Example example = new Example(Ticket.class);
        Example.Criteria criteria = example.createCriteria();

        if (userId != null) {
            if (type == 3) {
                criteria.andEqualTo("studentId", userId);
            }
            if (type == 2) {
                criteria.andEqualTo("coashId", userId);
            }
        }

        if (startDate != null && endDate != null) {
            criteria.andBetween("updateTime", startDate, endDate);
        }
        example.orderBy("updateTime").desc();

        List<Ticket> tickets = ticketMapper.selectByExample(example);
        int total = ticketMapper.selectCountByExample(example);


        List<Integer> ids = tickets.stream().map(ticket -> Stream.of(ticket.getStudentId(), ticket.getCoashId()).filter(Objects::nonNull)
                        .collect(Collectors.toList())).flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ids)) {
            Map<Integer, String> userIds = getUserIds(ids);
            tickets.stream().forEach(
                    ticket -> {
                        ticket.setCoashName(userIds.getOrDefault(ticket.getCoashId(), ""));
                        ticket.setStudentName(userIds.getOrDefault(ticket.getStudentId(), ""));
                    }
            );
        }

        return new TicketPageJsonOut(total,tickets);

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Ticket getTicketDetail(Integer id) throws BussinessErrorException, Exception {
        Ticket ticket = ticketMapper.selectByPrimaryKey(id);
        if (ticket == null || ticket.getDeleteTime() != null) {
            throw new BussinessErrorException("没有找到相关的信息");
        }
        List<Integer> ids = Arrays.asList(ticket.getStudentId(), ticket.getCoashId())
                .stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ids)) {
            Map<Integer, String> userIds = getUserIds(ids);
            ticket.setCoashName(userIds.getOrDefault(ticket.getCoashId(), ""));
            ticket.setStudentName(userIds.getOrDefault(ticket.getStudentId(), ""));
        }


        return ticket;
    }

    private Map<Integer,String> getUserIds(List<Integer> ids)  {
        Example example = new Example(LinUser.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andIn("id", ids);
        return linUserMapper.selectByExample(example)
                .stream().collect(Collectors.toMap(LinUser::getId,LinUser::getNickname));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Integer addmodelS(Ticket ticket
    ) throws BussinessErrorException, Exception {
        Example example = new Example(Ticket.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("studentId",ticket.getStudentId());
        criteria.andIsNotNull("coashId");
        List<Ticket> tickets = ticketMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(tickets)){
            throw new BussinessErrorException("已签到，签到教练为:" +linUserMapper.selectByPrimaryKey(tickets.stream().findAny().get().getCoashId()).getNickname());
        }
        ticket.setCreateTime(new Date());
        int insert = ticketMapper.insert(ticket);
        return ticket.getId();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Ticket updModelS(Ticket ticket) throws Exception {
        Ticket dbInfo = ticketMapper.selectByPrimaryKey(ticket.getId());
        if (dbInfo == null || dbInfo.getDeleteTime() != null) {
            throw new BussinessErrorException("没有找到相关的信息");
        }
        if ( dbInfo.getCoashId() != null) {
            LinUser linUser = linUserMapper.selectByPrimaryKey(dbInfo.getCoashId());
            throw new BussinessErrorException("二维码已被核销,核销人是" + linUser.getNickname());
        }
        dbInfo.setUserCount(ticket.getUserCount());
        dbInfo.setCoashId(ticket.getCoashId());
        dbInfo.setUpdateTime(new Date());
        ticketMapper.updateByPrimaryKey(dbInfo);
        Ticket ticketDetail = getTicketDetail(dbInfo.getId());
        ticketDetail.setUpdateTime(dbInfo.getUpdateTime());

        LinUser linUser = linUserMapper.selectByPrimaryKey(ticket.getStudentId());
        linUser.setCoach_id(ticketDetail.getCoashId());
        linUserMapper.updateByPrimaryKey(linUser);
        return ticketDetail;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void delModelS(Integer id) throws BussinessErrorException, Exception {
        Ticket dbInfo = ticketMapper.selectByPrimaryKey(id);
        if (dbInfo == null || dbInfo.getDeleteTime() != null) {
            throw new BussinessErrorException("没有找到相关的信息");
        }
        dbInfo.setDeleteTime(JdateUtils.getCurrentDate());
        ticketMapper.updateByPrimaryKey(dbInfo);
    }


    public Object logout(Integer uid) {
        return null;
    }
}
