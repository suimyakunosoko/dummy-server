package com.example.dummyserver.dto;

import com.example.dummyserver.exception.SystemException;
import com.example.dummyserver.utils.DummyServerUtils;
import java.io.IOException;
import javax.servlet.http.Part;
import lombok.Data;
import org.apache.commons.io.IOUtils;

@Data
public class PartDataInfo {

  public PartDataInfo() {
  }

  public PartDataInfo(Part part) {
    this.contentType = part.getContentType();
    this.name = part.getName();
    this.submittedFileName = part.getSubmittedFileName();
    try {
      this.body = IOUtils
          .toString(part.getInputStream(), DummyServerUtils.getCharset(this.contentType));
    } catch (IOException e) {
      throw new SystemException(e);
    }
  }

  private String contentType;

  private String name;

  private String submittedFileName;

  private String body;

}

