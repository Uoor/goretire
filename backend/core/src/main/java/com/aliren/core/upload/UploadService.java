package com.aliren.core.upload;

import com.aliren.core.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传（MVP 采用 ECS 本地静态目录，后续可平滑切换 OSS）：
 * 校验图片类型与大小 → 存入 aliren.upload.dir → 返回 /uploads/xxx 相对 URL
 * （由 WebConfig 静态资源映射 + 前端 dev 代理暴露）。
 */
@Slf4j
@Service
public class UploadService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path uploadDir;
    private final long maxBytes;

    public UploadService(@Value("${aliren.upload.dir:./uploads}") String dir,
                         @Value("${aliren.upload.max-size-mb:5}") long maxSizeMb) {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        this.maxBytes = maxSizeMb * 1024 * 1024;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDir, e);
        }
    }

    /** 存储图片，返回相对 URL（如 /uploads/xxxx.jpg） */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片不能超过 " + maxBytes / 1024 / 1024 + "MB");
        }
        String original = file.getOriginalFilename();
        String ext = original == null ? "" : original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/png/webp/gif 图片");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            file.transferTo(uploadDir.resolve(filename).toFile());
        } catch (IOException e) {
            log.error("upload failed", e);
            throw new BusinessException(500, "上传失败，请稍后再试");
        }
        return "/uploads/" + filename;
    }
}
