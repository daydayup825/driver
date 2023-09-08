package cn.chenxins.cms.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class UserDTO {

    private Integer id;

    private String nickname;

    private String card;

    private String phone;

    private String type;

    private BigDecimal cost;

    private String coach;

    private String introducer;

    private String subjectTne;

    private Short subjectTwo;

    private Date registerTime;
}
