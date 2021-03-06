package com.github.df.restypass.command;

import com.github.df.restypass.base.RestyConst;
import com.github.df.restypass.spring.pojo.PathVariableData;
import com.github.df.restypass.spring.pojo.RequestBodyData;
import com.github.df.restypass.spring.pojo.RequestHeaderData;
import com.github.df.restypass.spring.pojo.RequestParamData;
import com.github.df.restypass.util.CommonTools;
import com.github.df.restypass.util.JsonTools;
import com.github.df.restypass.util.StringBuilderFactory;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request 模板
 * Created by darrenfu on 17-6-25.
 */
@SuppressWarnings("unchecked")
@Data
public class RestyRequestTemplate {

    private static final Pattern pathVariableReg = Pattern.compile("\\{.+?\\}");

    private Method method;

    // GET POST
    private String httpMethod;

    // pat = baseUrl + methodUrl
    private String path;

    // @RequestMapping on class
    private String baseUrl;

    //@RequestMapping on method
    private String methodUrl;

    // @RequestMapping中定义的 Header
    private Map<String, String> headers;

    // @RequestMapping中定义的 params
    private Map<String, Object> params;

    //@RequestParam
    private List<RequestParamData> requestParams;

    //@PathVariable
    private List<PathVariableData> pathVariables;

    //@RequestBody参数 或者是没有注解的参数
    private List<RequestBodyData> requestBody;

    //@RequestHeader参数
    private List<RequestHeaderData> requestHeader;

    /**
     * Gets headers.
     *
     * @return the headers
     */
    public Map<String, String> getRequestHeaders(Object[] args) {
        if (CommonTools.isEmpty(headers)) {
            headers = new HashMap<>();
        }
        if (!headers.containsKey(RestyConst.CONTENT_TYPE)) {
            headers.put(RestyConst.CONTENT_TYPE, RestyConst.APPLICATION_JSON);
        }
        if (args != null && args.length > 0 && !CommonTools.isEmpty(requestHeader)) {
            for (RequestHeaderData headerData : requestHeader) {
                headers.put(headerData.getName(), ObjectUtils.defaultIfNull(String.valueOf(args[headerData.getIndex()]), headerData.getDefaultValue()));
            }
        }
        return headers;
    }

    /**
     * Add header.
     *
     * @param head  the head
     * @param value the value
     */
    public void addHeader(String head, String value) {
        if (headers == null) {
            headers = new HashMap();
        }
        headers.put(head, value);
    }

    /**
     * Add param.
     *
     * @param param the param
     * @param value the value
     */
    public void addParam(String param, String value) {
        if (params == null) {
            params = new HashMap();
        }
        params.put(param, value);
    }

    /**
     * Add request param.
     *
     * @param requestParamData the request param data
     */
    public void addRequestParam(RequestParamData requestParamData) {
        if (requestParams == null) {
            requestParams = new ArrayList<>();
        }
        requestParams.add(requestParamData);
    }


    /**
     * Add path variable.
     *
     * @param pathVariableData the path variable data
     */
    public void addPathVariable(PathVariableData pathVariableData) {
        if (pathVariables == null) {
            pathVariables = new ArrayList<>();
        }
        pathVariables.add(pathVariableData);
    }

    /**
     * Add request body.
     *
     * @param requestBodyData the request body data
     */
    public void addRequestBody(RequestBodyData requestBodyData) {
        if (requestBody == null) {
            requestBody = new ArrayList<>();
        }
        requestBody.add(requestBodyData);
    }

    /**
     * Add request header.
     *
     * @param requestHeaderData the request header data
     */
    public void addRequestHeader(RequestHeaderData requestHeaderData) {
        if (requestHeader == null) {
            requestHeader = new ArrayList<>();
        }
        requestHeader.add(requestHeaderData);
    }

    /**
     * Gets request path.
     *
     * @param args the args
     * @return the request path
     */
    public String getRequestPath(Object[] args) {
        if (this.pathVariables != null && this.pathVariables.size() > 0) {
            StringBuffer sb = new StringBuffer(64);
            Matcher matcher = pathVariableReg.matcher(this.path);
            while (matcher.find()) {
                String pathVariable = findPathVariable(matcher.group(), args);
                matcher.appendReplacement(sb, ObjectUtils.defaultIfNull(pathVariable, ""));
            }
            matcher.appendTail(sb);
            return sb.toString();
        } else {
            return this.path;
        }
    }

    /**
     * 获取 path variable
     *
     * @param placeholder {XXX}
     * @param args        object[] args
     * @return path value
     */
    private String findPathVariable(String placeholder, Object[] args) {
        for (PathVariableData pathVariable : this.pathVariables) {
            if (pathVariable.getName().equalsIgnoreCase(placeholder)) {
                if (args.length > pathVariable.getIndex() && args[pathVariable.getIndex()] != null) {
                    return args[pathVariable.getIndex()].toString();
                }
            }
        }
        return null;
    }


    /**
     * Gets query string.
     *
     * @param args the args
     * @return the query string
     */
    public String getQueryString(Object[] args) {
        if (CommonTools.isEmpty(this.params) && CommonTools.isEmpty(this.requestParams)) {
            return null;
        }
        StringBuilder sb = StringBuilderFactory.DEFAULT.stringBuilder();
        int index = 0;

        if (!CommonTools.isEmpty(this.getParams())) {
            // 处理Params的值
            Set<String> paramNames = this.getParams().keySet();

            for (String paramName : paramNames) {
                Object paramValue = this.getParams().getOrDefault(paramName, "");
                if (paramValue != null && StringUtils.isNotEmpty(paramValue.toString())) {
                    if (index != 0) {
                        sb.append("&");
                    }
                    sb.append(paramName);
                    sb.append("=");
                    sb.append(paramValue);
                    index++;
                }
            }
        }

        if (!CommonTools.isEmpty(this.requestParams)) {
            // 处理RequestParam的值
            for (RequestParamData requestParam : this.requestParams) {
                Object arg = args[requestParam.getIndex()];
                if (arg == null) {
                    arg = requestParam.getDefaultValue();
                }
                if (arg != null && StringUtils.isNotEmpty(arg.toString())) {
                }
                if (index != 0) {
                    sb.append("&");
                }
                sb.append(requestParam.getName());
                sb.append("=");
                sb.append(arg.toString());
                index++;
            }

        }
        return sb.toString();
    }


    /**
     * Gets body.
     *
     * @param args the args
     * @return the body
     */
    public String getBody(Object[] args) {
        if (RestyConst.HTTP_POST.equalsIgnoreCase(this.httpMethod)) {
            // POST 请求 没有请求参数时，body -> {}
            if (CommonTools.isEmpty(this.requestBody)) {
                return "{}";
            }
            // body的参数数量一个
            if (this.requestBody.size() == 1) {
                return JsonTools.nonNullMapper().toJson(args[this.requestBody.get(0).getIndex()]);
            }

            // body的参数数量多个
            Map<String, Object> bodyMap = new HashMap<>();
            for (RequestBodyData bodyData : this.requestBody) {
                bodyMap.put(bodyData.getName(), args[bodyData.getIndex()]);
            }
            return JsonTools.nonNullMapper().toJson(bodyMap);
        } else {
            return null;
        }
    }

}
