package cn.chenxins.cms.controller;

import cn.chenxins.authorization.annotation.LoginRequired;
import cn.chenxins.cms.model.entity.Ticket;
import cn.chenxins.cms.service.TicketService;
import cn.chenxins.exception.BussinessErrorException;
import cn.chenxins.utils.ResultJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("drive/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;


    @GetMapping("{id}")
    @LoginRequired
    public Object getBookDetail(@RequestHeader(value = "authorization",required = false) String authorization,@PathVariable(required = false) Integer id) {
        try {
            if (id == null) {
                return ResultJson.ParameterException("id是必填项！", id);
            }
            return ticketService.getTicketDetail(id);
        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }


    @PostMapping("/")
    public Object createTicket(@RequestHeader(value = "ticket",required = false) String authorization,@RequestBody Ticket ticket) {

        try {
            Integer integer = ticketService.addmodelS(ticket);
            return ResultJson.Sucess(integer);

        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }

    @PutMapping("{id}")
    @LoginRequired
    public Object updateBook(@RequestHeader(value = "authorization",required = false) String authorization,@PathVariable Integer id, @RequestBody Ticket ticket) {

        try {
            Ticket ticketDetail = ticketService.updModelS(ticket);
            return ResultJson.Sucess(ticketDetail);

        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }

    }

    @DeleteMapping(value = "{id}", name = "图书#删除图书")
    @LoginRequired
    public Object deleteBook(@RequestHeader(value = "authorization",required = false) String authorization,@PathVariable Integer id) {
        try {
            ticketService.delModelS(id);
            return ResultJson.Sucess();

        } catch (BussinessErrorException be) {
            return ResultJson.BussinessException(be.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultJson.ServerError();
        }
    }


}
