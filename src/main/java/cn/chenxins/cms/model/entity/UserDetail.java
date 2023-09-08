package cn.chenxins.cms.model.entity;

import cn.chenxins.cms.model.json.UserJsonIn;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

public class UserDetail {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "coach")
    private Integer coach_id;

    @Column(name = "introducer")
    private String introducer;

    @Column(name = "subject_one")
    private String subjectTne;

    @Column(name = "subject_two")
    private Short subjectTwo;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "register_time")
    private Date registerTime;

    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "delete_time")
    private Date deleteTime;

}
