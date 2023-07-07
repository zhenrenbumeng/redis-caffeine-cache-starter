package org.pw.l2cache.po;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class User {
    private Integer id;
    private String name;

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
