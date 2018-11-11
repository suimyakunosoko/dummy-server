package com.example.dummyserver.controller;


import com.example.dummyserver.dto.RequestDto;
import com.example.dummyserver.service.DummyServerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ダミー用のサーブレット
 *
 * 処理想定
 * リクエスト受信
 * ↓
 * サーブレットパスから設定ファイル読み込み
 * ↓
 * 設定ファイルから返却リクエスト生成
 */
@Controller
@Slf4j
public class DummyServerController {

  private ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private DummyServerService service;

  @RequestMapping("/*")
  public ResponseEntity<?> dummyServerMain(HttpServletRequest request) {
    RequestDto requestDto = new RequestDto(request);
    requestDto.showInfo();
    return service.createResponseEntity(requestDto);
  }

}
