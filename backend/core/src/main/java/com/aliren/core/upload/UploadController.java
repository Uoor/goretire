package com.aliren.core.upload;

import com.aliren.core.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 图片上传（MVP 本地存储，返回 /uploads/ 相对 URL） */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /** multipart 上传（常规 WebKit/标准浏览器路径） */
    @PostMapping
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(uploadService.store(file));
    }

    /**
     * base64 上传（华为 UWS 等 WebView 内核：multipart/FormData 不可靠，社区通行方案为 FileReader + JSON）。
     */
    @PostMapping("/base64")
    public ApiResponse<String> uploadBase64(@RequestBody Base64UploadRequest req) {
        return ApiResponse.ok(uploadService.storeBase64(req.getName(), req.getData()));
    }
}
