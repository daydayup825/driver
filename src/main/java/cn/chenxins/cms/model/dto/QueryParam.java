package cn.chenxins.cms.model.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class QueryParam {
    private String nickname;
    private Integer type;
    private Integer subjectThree;
    private Integer subjectTwo;
    private String introducer;
    private Integer page;
    private Integer count;
    private String startDate;
    private String endDate;

    private Integer searchTimerangeType;

    private String coachName;



}
