package cn.chenxins.cms.model.json;

import cn.chenxins.cms.model.entity.LinUser;

import java.util.List;

public class UserPageJsonOut {

    private Integer total_nums;

    private List<LinUser> collection;

    public UserPageJsonOut(Integer total_nums, List<LinUser> collection) {
        this.total_nums = total_nums;
        this.collection = collection;
    }

    public Integer getTotal_nums() {
        return total_nums;
    }

    public void setTotal_nums(Integer total_nums) {
        this.total_nums = total_nums;
    }

    public List<LinUser> getCollection() {
        return collection;
    }

    public void setCollection(List<LinUser> collection) {
        this.collection = collection;
    }
}
