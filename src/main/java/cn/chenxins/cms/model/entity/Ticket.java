package cn.chenxins.cms.model.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Table(name = "ticket")
@Data
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer studentId;

    @Transient
    private String studentName;

    private Integer userCount;

    private Integer coashId;

    @Transient
    private String coashName;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "delete_time")
    private Date deleteTime;

}