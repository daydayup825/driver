package cn.chenxins.cms.model.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class QueryParam {
    private String nickName;
    private Integer type;
    private Integer subjectThree;
    private Integer subjectTwo;
    private String coachName;
    private Integer page;
    private Integer count;
    private String startDate;
    private String endDate;

    private Integer searchTimerangeType;



}
