package cn.chenxins.cms.model.json;

import cn.chenxins.cms.model.entity.LinUser;
import cn.chenxins.cms.model.entity.Ticket;

import java.util.List;

public class TicketPageJsonOut {

    private Integer total_nums;

    private List<Ticket> collection;

    public TicketPageJsonOut(Integer total_nums, List<Ticket> collection) {
        this.total_nums = total_nums;
        this.collection = collection;
    }

    public Integer getTotal_nums() {
        return total_nums;
    }

    public void setTotal_nums(Integer total_nums) {
        this.total_nums = total_nums;
    }

    public List<Ticket> getCollection() {
        return collection;
    }

    public void setCollection(List<Ticket> collection) {
        this.collection = collection;
    }
}
