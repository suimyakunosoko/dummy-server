package com.example.dummyserver.dto;

import com.example.dummyserver.exception.SystemException;
import com.example.dummyserver.utils.DummyServerUtils;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Data
@Slf4j
public class RequestDto {

  public RequestDto(HttpServletRequest request) {

    // 基本情報
    this.servletPath = request.getServletPath();
    this.authType = request.getAuthType();
    this.cookies = DummyServerUtils.toList(request.getCookies());
    this.headers = DummyServerUtils
        .toMap(request.getHeaderNames(), key -> Collections.list(request.getHeaders(key)));
    this.method = request.getMethod();
    this.contentType = request.getContentType();
    this.charset = DummyServerUtils.getCharset(this.contentType);
    // BODY情報
    this.queryStrings = Objects.nonNull(request.getQueryString()) ? new HashSet<>(
        Arrays.asList(request.getQueryString().split("&"))) : new HashSet<>();
    try {
      this.parts =
          DummyServerUtils.isMultiPartRequest(contentType) ? request.getParts().stream()
              .map(PartDataInfo::new).collect(
                  Collectors.toSet()) : new HashSet<>();
      this.json = DummyServerUtils.isJsonRequest(contentType) ?
          DummyServerUtils.toJson(request.getInputStream(),
              this.charset)
          : new ConcurrentHashMap<>();
      this.text = DummyServerUtils.isTextRequest(contentType) ?
          IOUtils.toString(request.getInputStream(),
              this.charset)
          : "";
    } catch (ServletException e) {
      throw new SystemException(e);
    } catch (IOException e) {
      throw new SystemException(e);
    }
  }

  private String authType;

  private List<Cookie> cookies;

  private Map<String, List<String>> headers;

  private String method;

  private String contentType;

  private Charset charset;

  private Set<String> queryStrings;

  private Collection<PartDataInfo> parts;

  private Map<String, String> json;

  private String text;

  private String servletPath;

  public void showInfo() {
    log.info("servletPath: {}", servletPath);
    log.info("authType: {}", authType);
    log.info("cookies: {}", cookies);
    log.info("headers: {}", headers);
    log.info("requestMethod: {}", method);
    log.info("contentType: {}", contentType);
    log.info("queryString: {}", this.queryStrings);
    log.info("parts: {}", parts);
    log.info("json: {}", json);
    log.info("text: {}", text);
  }
}
