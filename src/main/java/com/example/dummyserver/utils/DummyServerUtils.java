package com.example.dummyserver.utils;

import com.example.dummyserver.exception.SystemException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.http.HttpProperties.Encoding;

public class DummyServerUtils {

  private static final Pattern MULTIPART_PATTERN = Pattern.compile("^multipart/(form-data|mixed stream)", Pattern.CASE_INSENSITIVE);
  private static final Pattern JSON_PATTERN = Pattern.compile("^application/json", Pattern.CASE_INSENSITIVE);
  private static final Pattern TEXT_PATTERN = Pattern.compile("^text/", Pattern.CASE_INSENSITIVE);
  private static final Pattern FIND_CHARSET_PATTERN = Pattern.compile("^.*charset=(.+?)(;.*$|$)", Pattern.CASE_INSENSITIVE);
  private static final ObjectMapper MAPPER = new ObjectMapper();


  public static <T> boolean isNullMixed(T... objects) {
    long count = Arrays.stream(objects).filter(Objects::nonNull).count();
    return count > 0 && count != objects.length;
  }

  public static boolean equalsOrExcludedString(String target, String expect) {
    if (Objects.isNull(expect)) {
      return true;
    }
    return StringUtils.equals(target, expect);
  }

  public static <T> List<T> toList(T[] src) {
    return Objects.nonNull(src) ? Arrays.asList(src) : new ArrayList<>();
  }

  public static <K,V> Map<K, V> toMap(Enumeration<K> keys, Function<K, V> valueFunction) {
    return toMap(Collections.list(keys), valueFunction);
  }

  public static <K,V> Map<K, V> toMap(List<K> keys, Function<K, V> valueFunction) {
    if (Objects.isNull(keys) || Objects.isNull(valueFunction)) {
      return new ConcurrentHashMap<>();
    }
    return keys.stream().collect(Collectors.toMap(key -> key, valueFunction));
  }

  public static boolean isMultiPartRequest(String contentType) {
    return Objects.nonNull(contentType) && MULTIPART_PATTERN.matcher(contentType).find();
  }

  public static boolean isJsonRequest(String contentType) {
    return Objects.nonNull(contentType) && JSON_PATTERN.matcher(contentType).find();
  }

  public static boolean isTextRequest(String contentType) {
    return Objects.nonNull(contentType) && TEXT_PATTERN.matcher(contentType).find();
  }

    public static Charset getCharset(String contentType) {
    Charset charset = null;
    if (Objects.nonNull(contentType)) {
      charset = Charsets.toCharset(FIND_CHARSET_PATTERN.matcher(contentType).replaceAll("$1"));
    }
    return Objects.nonNull(charset) ? charset : Charset.defaultCharset();
  }

  public static Map<String, String> toJson(InputStream inputStream, Charset charset) {
    try {
      return MAPPER.readValue(IOUtils.toString(inputStream, charset), ConcurrentHashMap.class);
    } catch (IOException e) {
      throw new SystemException(e);
    }
  }

}
