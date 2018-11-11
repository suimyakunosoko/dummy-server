package com.example.dummyserver.service;

import com.example.dummyserver.dto.DataTestDto;
import com.example.dummyserver.dto.PartDataInfo;
import com.example.dummyserver.dto.RequestDto;
import com.example.dummyserver.dto.ResponseInfoDto;
import com.example.dummyserver.dto.ResponseSelectorsInfo;
import com.example.dummyserver.exception.SystemException;
import com.example.dummyserver.utils.DummyServerUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.servlet.http.Cookie;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriUtils;
import org.yaml.snakeyaml.Yaml;

@Service
public class DummyServerService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String RESOURCE_PATH = ResourceLoader.CLASSPATH_URL_PREFIX + "response";

  private static final String RESPONSE_SETTING_FILE = "/setting.yml";

  private static final String HTTP_HEADERS_CONTENT_DISPOSITON = "Content-Disposition";

  private static final String CONTENT_DISPOSITION_FORMAT = "attachment; filename=\"%s\"; filename*=UTF-8''%s";

  @Autowired
  private ResourceLoader resourceLoader;

  Yaml yaml = new Yaml();

  public ResponseEntity<?> createResponseEntity(RequestDto request) {
    ResponseSelectorsInfo selectorDto = loadSelectorDto(request);
    ResponseInfoDto responseInfo = findResponseInfo(request, selectorDto);
    if (Objects.isNull(responseInfo)) {
      return null;
    }
    switch (responseInfo.getType()) {
      case TEXT:
        return createTextResponse(responseInfo);
      case JSON:
        return createJsonResponse(responseInfo);
      case FILE:
        return createFileResponse(responseInfo, request.getServletPath());
      default:
        return null;
    }
  }

  private ResponseSelectorsInfo loadSelectorDto(RequestDto request) {
    Resource resource = resourceLoader
        .getResource(DummyServerUtils
            .toFilePath(RESOURCE_PATH, request.getServletPath(), RESPONSE_SETTING_FILE));
    if (!resource.exists()) {
      throw new SystemException("file not found.");
    }
    try (Reader is = new InputStreamReader(new FileInputStream(resource.getFile()),
        StandardCharsets.UTF_8)) {
      return yaml.loadAs(is,
          ResponseSelectorsInfo.class);
    } catch (IOException e) {
      throw new SystemException(e);
    }
  }

  private ResponseInfoDto findResponseInfo(RequestDto request, ResponseSelectorsInfo selectorsDto) {
    return selectorsDto.getResponses().stream()
        .filter(p -> matchString(p.getAuthType(), request.getAuthType()))
        .filter(p -> matchCookies(p.getCookies(), request.getCookies()))
        .filter(p -> matchHeaders(p.getHeaders(), request.getHeaders()))
        .filter(p -> matchString(p.getMethod(), request.getMethod()))
        .filter(p -> matchString(p.getContentType(), request.getContentType()))
        .filter(p -> matchCharset(p.getCharset(), request.getCharset()))
        .filter(p -> matchQueryString(p.getQueryStrings(), request.getQueryStrings()))
        .filter(p -> matchParts(p.getParts(), request.getParts()))
        .filter(p -> matchJson(p.getJson(), request.getJson()))
        .filter(p -> matchString(p.getText(), request.getText()))
        .findFirst().get().getResponseInfo();
  }

  private <T> boolean matchCollection(Set<DataTestDto<Collection<T>>> testList,
      Collection<T> list, BiPredicate<T, T> biPredicate) {
    if (Objects.isNull(testList)) {
      return true;
    }
    return testList.stream().anyMatch(p -> {
      Collection<T> expect = p.getExpect();
      switch (p.getType()) {
        case EQUAL:
          return equalsCollection(list, expect, (p1, p2) -> biPredicate.test(p1, p2));
        case NOT_EQUAL:
          return !equalsCollection(list, expect, (p1, p2) -> biPredicate.test(p1, p2));
        case CONTAIN:
          return expect.stream()
              .anyMatch(p1 -> list.stream().anyMatch(p2 -> biPredicate.test(p1, p2)));
        case NOT_CONTAIN:
          return !expect.stream()
              .anyMatch(p1 -> list.stream().anyMatch(p2 -> biPredicate.test(p1, p2)));
        default:
          return false;
      }
    });
  }

  private <K, V> boolean matchMap(Set<DataTestDto<Map<K, V>>> testHeaders,
      Map<K, V> headers, BiPredicate<V, V> predicate) {
    if (Objects.isNull(testHeaders)) {
      return true;
    }
    return testHeaders.stream().anyMatch(p -> {
      Map<K, V> expect = p.getExpect();
      switch (p.getType()) {
        case EQUAL:
          return equalsMap(headers, expect, (a, b) -> predicate.test(a, b));
        case NOT_EQUAL:
          return !equalsMap(headers, expect, (a, b) -> predicate.test(a, b));
        case CONTAIN:
          return headers.keySet().stream()
              .anyMatch(key -> predicate.test(headers.get(key), expect.get(key)));
        case NOT_CONTAIN:
          return !headers.keySet().stream()
              .anyMatch(key -> predicate.test(headers.get(key), expect.get(key)));
        default:
          return false;
      }
    });
  }

  private boolean matchString(Set<DataTestDto<String>> testAuthType, String authType) {
    if (Objects.isNull(testAuthType)) {
      return true;
    }
    return testAuthType.stream().anyMatch(p -> {
      switch (p.getType()) {
        case EQUAL:
          return Objects.equals(authType, p.getExpect());
        case NOT_EQUAL:
          return !Objects.equals(authType, p.getExpect());
        case CONTAIN:
          return StringUtils.contains(authType, p.getExpect());
        case NOT_CONTAIN:
          return !StringUtils.contains(authType, p.getExpect());
        default:
          return false;
      }
    });
  }

  private boolean matchCookies(Set<DataTestDto<List<Cookie>>> testCookies, List<Cookie> cookies) {
    if (Objects.isNull(testCookies)) {
      return true;
    }
    return testCookies.stream().anyMatch(p -> {
      List<Cookie> expect = p.getExpect();
      switch (p.getType()) {
        case EQUAL:
          return equalsCollection(cookies, expect, (a, b) -> equalCookie(a, b));
        case NOT_EQUAL:
          return !equalsCollection(cookies, expect, (a, b) -> equalCookie(a, b));
        case CONTAIN:
          return cookies.stream()
              .anyMatch(p1 -> expect.stream().anyMatch(p2 -> equalCookie(p1, p2)));
        case NOT_CONTAIN:
          return !cookies.stream()
              .anyMatch(p1 -> expect.stream().anyMatch(p2 -> equalCookie(p1, p2)));
        default:
          return false;
      }
    });
  }

  private boolean equalCookie(Cookie a, Cookie b) {
    return Objects.equals(a, b)
        && Objects.nonNull(a)
        && Objects.nonNull(b)
        && Objects.equals(a.getName(), b.getName())
        && Objects.equals(a.getValue(), b.getValue());
  }

  private boolean matchHeaders(Set<DataTestDto<Map<String, List<String>>>> testHeaders,
      Map<String, List<String>> headers) {
    return matchMap(testHeaders, headers,
        (a, b) -> equalsCollection(a, b, (p1, p2) -> StringUtils.equals(p1, p2)));
  }

  private boolean matchCharset(Set<DataTestDto<Charset>> testCharset, Charset Charset) {
    if (Objects.isNull(testCharset)) {
      return true;
    }
    return testCharset.stream().anyMatch(p -> {
      switch (p.getType()) {
        case EQUAL:
        case CONTAIN:
          return Objects.equals(Charset, p.getExpect());
        case NOT_EQUAL:
        case NOT_CONTAIN:
          return !Objects.equals(Charset, p.getExpect());
        default:
          return false;
      }
    });
  }

  private boolean matchQueryString(Set<DataTestDto<Collection<String>>> testQuery,
      Set<String> query) {
    return matchCollection(testQuery, query, (p1, p2) -> StringUtils.equals(p1, p2));
  }

  private boolean matchParts(Set<DataTestDto<Collection<PartDataInfo>>> testParts,
      Collection<PartDataInfo> parts) {
    return matchCollection(testParts, parts, (p1, p2) -> {
      if (DummyServerUtils.isNullMixed(p1, p2)) {
        return false;
      }
      return StringUtils.equals(p1.getName(), p2.getName())
          && StringUtils.equals(p1.getSubmittedFileName(), p2.getSubmittedFileName())
          && StringUtils.equals(p1.getContentType(), p2.getContentType())
          && StringUtils.equals(p1.getBody(), p2.getBody());
    });
  }

  private boolean matchJson(Set<DataTestDto<Map<String, String>>> testJson,
      Map<String, String> json) {
    return matchMap(testJson, json, (p1, p2) -> StringUtils.equals(p1, p2));
  }


  private <K, V> boolean equalsMap(Map<K, V> a, Map<K, V> b, BiPredicate<V, V> biPredicate) {
    if (DummyServerUtils.isNullMixed(a, b)) {
      return false;
    }
    if (a.size() != b.size()) {
      return false;
    }

    for (K key : b.keySet()) {
      if (!biPredicate.test(a.get(key), b.get(key))) {
        return false;
      }
    }
    return true;
  }

  private <T> boolean equalsCollection(Collection<T> a, Collection<T> b,
      BiPredicate<T, T> biPredicate) {
    if (DummyServerUtils.isNullMixed(a, b)) {
      return false;
    }
    if (a.size() != b.size()) {
      return false;
    }
    Collection<T> copy = new HashSet<>(a);
    for (T child : b) {
      Optional<T> optional = copy.stream().filter(p -> biPredicate.test(child, p)).findFirst();
      if (!optional.isPresent()) {
        return false;
      }
      copy.remove(optional.get());
    }
    return true;
  }

  private ResponseEntity<String> createTextResponse(ResponseInfoDto responseInfo) {
    return ResponseEntity.status(responseInfo.getStatus())
        .headers(toHttpHeaders(responseInfo.getHttpHeaders()))
        .contentType(MediaType.TEXT_PLAIN).body(responseInfo.getResponseText());
  }

  private ResponseEntity<String> createJsonResponse(ResponseInfoDto responseInfo) {
    try {
      return ResponseEntity.status(responseInfo.getStatus())
          .headers(toHttpHeaders(responseInfo.getHttpHeaders()))
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .body(MAPPER.writeValueAsString(responseInfo.getResponseJson()));
    } catch (JsonProcessingException e) {
      throw new SystemException(e);
    }
  }

  private ResponseEntity<byte[]> createFileResponse(ResponseInfoDto responseInfo,
      String servletPath) {
    Resource resource = resourceLoader
        .getResource(DummyServerUtils
            .toFilePath(RESOURCE_PATH, servletPath, responseInfo.getResponseFile()));
    try (InputStream input = new FileInputStream(resource.getFile())) {
      HttpHeaders httpHeaders = toHttpHeaders(responseInfo.getHttpHeaders());
      changeContentDisposition(httpHeaders, responseInfo.getResponseFile(),
          responseInfo.getResponseFileName());
      return ResponseEntity.status(responseInfo.getStatus())
          .headers(httpHeaders)
          .contentType(MediaType.APPLICATION_OCTET_STREAM).body(IOUtils.toByteArray(input));
    } catch (IOException e) {
      throw new SystemException(e);
    }
  }

  private HttpHeaders toHttpHeaders(Map<String, List<String>> map) {
    return new HttpHeaders(new LinkedMultiValueMap<>(map));
  }

  private HttpHeaders changeContentDisposition(HttpHeaders httpHeaders, String originalFileName,
      String fileName) {
    ContentDisposition disposition = httpHeaders.getContentDisposition();
    if (Objects.isNull(disposition)
        || Objects.isNull(disposition.getFilename())
        || Objects.isNull(disposition.getName())) {
      fileName = Objects.isNull(fileName) ? originalFileName
          : fileName;
      httpHeaders.remove(HTTP_HEADERS_CONTENT_DISPOSITON);
      httpHeaders.add(HTTP_HEADERS_CONTENT_DISPOSITON,
          String.format(CONTENT_DISPOSITION_FORMAT, fileName, UriUtils
              .encode(fileName, StandardCharsets.UTF_8.name())));
    }
    return httpHeaders;
  }
}
