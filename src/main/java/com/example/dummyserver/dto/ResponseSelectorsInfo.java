package com.example.dummyserver.dto;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.Part;
import lombok.Data;

@Data
public class ResponseSelectorsInfo {

  private List<ResponseSelectorInfo> responses;

  @Data
  public static class ResponseSelectorInfo {
    private Set<DataTestDto<String>> authType;

    private Set<DataTestDto<List<Cookie>>> cookies;

    private Set<DataTestDto<Map<String, List<String>>>> headers;

    private Set<DataTestDto<String>> method;

    private Set<DataTestDto<String>> contentType;

    private Set<DataTestDto<Charset>> charset;

    private Set<DataTestDto<Collection<String>>> queryStrings;

    private Set<DataTestDto<Collection<PartDataInfo>>> parts;

    private Set<DataTestDto<Map<String, String>>> json;

    private Set<DataTestDto<String>> text;

    private ResponseInfoDto responseInfo;
  }
}
