package com.example.dummyserver.dto;

import com.example.dummyserver.constant.ResponseType;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;

@Data
public class ResponseInfoDto {

  private HttpStatus status;

  private Map<String, List<String>> httpHeaders;

  private ResponseType type;

  private String responseText;

  private Object responseJson;

  private String responseFile;

  private String responseFileName;
}
