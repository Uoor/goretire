package com.aliren.core.auth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DingTalkJsApiServiceTest {

    private final DingTalkJsApiService service =
            new DingTalkJsApiService("appKey", "appSecret", "123456", "corpId");

    /** 钉钉官方文档示例向量：字段按 ASCII 升序（jsapi_ticket < noncestr < timestamp < url）以 & 拼接后 SHA1 */
    @Test
    void signWithTicket_按官方规则签名() {
        String signature = service.signWithTicket(
                "mS5k98fdkdgDKxkXGEs8LORVREiweeWETE40P37wkidkfksDSKDJFD5h9nbSlYy3-Sl-HhTdfl2fzFy1AOcKIDU8l",
                "Zn4zmLFKD0wzilzM", "1414588745", "//open.dingtalk.com");
        assertEquals("653ecdeadf70a480b1aefa687c894a2d8ff9a8bb", signature);
    }

    /** url 的 query 部分需先做一次 urldecode 再参与签名 */
    @Test
    void signWithTicket_urlQuery做一次urldecode() {
        String decoded = DingTalkJsApiService.urldecodeQuery("https://abc.com?url=http%3A%2F%2Fabc.com%2Fsomewhere&a=1");
        assertEquals("https://abc.com?url=http://abc.com/somewhere&a=1", decoded);
    }

    @Test
    void buildSignResult_返回ddConfig所需字段() {
        Map<String, Object> result = service.buildSignResult("ticket", "https://example.com/house/1");
        assertEquals("123456", result.get("agentId"));
        assertEquals("corpId", result.get("corpId"));
        assertNotNull(result.get("nonceStr"));
        assertNotNull(result.get("timeStamp"));
        assertNotNull(result.get("signature"));
    }
}
