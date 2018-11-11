package com.example.dummyserver.dto;

import com.example.dummyserver.constant.TestType;
import lombok.Data;

@Data
public class DataTestDto<T> {

  private TestType type;

  private T expect;



}
