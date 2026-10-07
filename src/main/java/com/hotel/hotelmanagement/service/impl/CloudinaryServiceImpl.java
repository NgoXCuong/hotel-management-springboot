package com.hotel.hotelmanagement.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hotel.hotelmanagement.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadSingleImage(MultipartFile file, String folderPath) {
        validateImageFile(file);

        try {
            Map<?, ?> uploadParams = ObjectUtils.asMap(
                    "folder", folderPath != null ? folderPath.trim() : "hotel-management",
                    "resource_type", "image",
                    "use_filename", true,
                    "unique_filename", true
            );

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded image to Cloudinary successfully. Folder: {}, URL: {}", folderPath, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Lỗi trong quá trình upload ảnh lên Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> uploadMultipleImages(List<MultipartFile> files, String folderPath) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String url = uploadSingleImage(file, folderPath);
                if (url != null && !url.isBlank()) {
                    uploadedUrls.add(url);
                }
            }
        }
        return uploadedUrls;
    }

    @Override
    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null && !publicId.isBlank()) {
                Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted image on Cloudinary. Public ID: {}, Result: {}", publicId, result.get("result"));
            }
        } catch (Exception e) {
            log.warn("Could not delete image from Cloudinary (URL: {}): {}", imageUrl, e.getMessage());
        }
    }

    /**
     * Trích xuất public_id kèm folder path từ URL Cloudinary.
     * Ví dụ:
     * https://res.cloudinary.com/demo/image/upload/v1612345678/hotel-management/room-types/sample.jpg
     * -> hotel-management/room-types/sample
     */
    private String extractPublicIdFromUrl(String imageUrl) {
        int uploadIndex = imageUrl.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }

        String path = imageUrl.substring(uploadIndex + "/upload/".length());

        // Loại bỏ version tag nếu có (ví dụ: v1724400000/)
        path = path.replaceFirst("^v[0-9]+/", "");

        // Loại bỏ phần mở rộng đuôi file (.jpg, .png, .webp...)
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex != -1) {
            path = path.substring(0, lastDotIndex);
        }

        return path;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file hình ảnh.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng ảnh vượt quá giới hạn tối đa 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Tên file không hợp lệ hoặc thiếu phần mở rộng.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Chỉ chấp nhận các định dạng: JPG, JPEG, PNG, WEBP.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Loại nội dung file không hợp lệ (" + contentType + "). Vui lòng chọn file ảnh hợp lệ.");
        }
    }
}
