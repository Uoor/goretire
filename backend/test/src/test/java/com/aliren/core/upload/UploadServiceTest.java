package com.aliren.core.upload;

import com.aliren.core.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    private UploadService newService(long maxSizeMb) {
        return new UploadService(tempDir.toString(), maxSizeMb);
    }

    @Test
    void storeBase64_valid_savesFile() {
        UploadService svc = newService(5);
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03};
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);

        String url = svc.storeBase64("photo.jpg", dataUrl);

        assertThat(url).startsWith("/uploads/").endsWith(".jpg");
        assertThat(tempDir.resolve(url.substring("/uploads/".length()))).exists();
    }

    @Test
    void storeBase64_badExt_rejected() {
        UploadService svc = newService(5);
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);

        assertThatThrownBy(() -> svc.storeBase64("photo.heic", dataUrl))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void storeBase64_notDataUrl_rejected() {
        UploadService svc = newService(5);

        assertThatThrownBy(() -> svc.storeBase64("photo.jpg", "not-a-data-url"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void storeBase64_tooLarge_rejected() {
        UploadService svc = newService(1);
        byte[] big = new byte[2 * 1024 * 1024];
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(big);

        assertThatThrownBy(() -> svc.storeBase64("photo.jpg", dataUrl))
                .isInstanceOf(BusinessException.class);
    }
}
