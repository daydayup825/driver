package cn.chenxins.cms.model.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Date;

@Table(name = "lin_user")
@Data
public class LinUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "delete_time")
    private Date deleteTime;

    private String nickname;

    private String card;

    private String phone;

    // 1学生
    // 2教练
    // 3管理员
    // 0 admin
    private Integer type;


    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "coach_id")
    private Integer coach_id;

    @Transient
    private String coachName;

    @Column(name = "introducer")
    private String introducer;

    @Column(name = "subject_one")
    private Integer subjectOne;

    @Column(name = "subject_two")
    private Integer subjectTwo;

    @Column(name = "register_time")
    private Date registerTime;


}