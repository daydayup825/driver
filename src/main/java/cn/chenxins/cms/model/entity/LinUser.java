package cn.chenxins.cms.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // 0 没考 1考了 2没考过
    @Column(name = "subject_two")
    private Integer subjectTwo;

    @Column(name = "subject_two_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date subjectTwoTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "subject_three_time")
    private Date subjectThreeTime;

    @Column(name = "subject_three")
    private Integer subjectThree;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "register_time")
    private Date registerTime;

    @Transient
    private String registerTimeStr;





}