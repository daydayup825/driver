package cn.chenxins.cms.model.json;

import cn.chenxins.cms.model.entity.LinUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TokenJsonOut {

    private String access_token;

    private String refresh_token;

    private LinUser user;


}
