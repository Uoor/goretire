package com.aliren.core.upload;

import lombok.Data;

/** base64 图片上传请求体：name=原文件名（用于扩展名校验），data=data URL（data:image/xxx;base64,...） */
@Data
public class Base64UploadRequest {

    private String name;

    private String data;
}
