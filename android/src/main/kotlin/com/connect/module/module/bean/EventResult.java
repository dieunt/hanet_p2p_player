package com.connect.module.module.bean;

/**
 * Created by ly on 2020/11/10.
 * Describe: EventBus 数据通用模版
 */
public class EventResult {

    public static int RESULT_SUCCESS = 0;
    public static int RESULT_ERROR = -1;

    public static EventResult getResult(String url, int requestCode, Object o) {
        EventResult r = new EventResult();
        r.setRequestUrl(url);
        r.setResponseCode(requestCode);
        r.setObject(o);
        return r;
    }

    /**
     * 过滤消息标记（唯一）
     * 请求的 UIR 或 数据传输的唯一标记
     */
    private String requestUrl = "";

    /**
     * 请求结果是否成功标记
     * 0 成功； 其他失败
     */
    private int responseCode = RESULT_ERROR;

    /**
     * 请求结果（元数据类型），自我做数据转换
     */
    private Object object;

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "EventResult{" +
                "requestUrl='" + requestUrl + '\'' +
                ", requestCode=" + responseCode +
                ", object=" + object +
                '}';
    }
}
