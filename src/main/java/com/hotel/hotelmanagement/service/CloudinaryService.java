package com.hotel.hotelmanagement.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {

    /**
     * Upload 1 ảnh lên Cloudinary.
     * Kiểm tra định dạng (jpg, jpeg, png, webp), dung lượng tối đa 5MB.
     * @param file file ảnh cần upload
     * @param folderPath thư mục lưu trữ trên Cloudinary (vd: hotel-management/room-types)
     * @return secure_url của ảnh sau khi upload
     */
    String uploadSingleImage(MultipartFile file, String folderPath);

    /**
     * Upload danh sách nhiều ảnh cùng lúc lên Cloudinary.
     * @param files danh sách file ảnh
     * @param folderPath thư mục lưu trữ trên Cloudinary
     * @return danh sách secure_url của các ảnh đã upload thành công
     */
    List<String> uploadMultipleImages(List<MultipartFile> files, String folderPath);

    /**
     * Xóa ảnh trên Cloudinary theo URL.
     * Tự động trích xuất public_id (bao gồm cả thư mục) và gọi lệnh destroy.
     * @param imageUrl URL của ảnh trên Cloudinary
     */
    void deleteImageByUrl(String imageUrl);

    // Alias methods for convenience
    default String uploadImage(MultipartFile file, String folderPath) {
        return uploadSingleImage(file, folderPath);
    }

    default List<String> uploadImages(List<MultipartFile> files, String folderPath) {
        return uploadMultipleImages(files, folderPath);
    }

    default void deleteImage(String imageUrl) {
        deleteImageByUrl(imageUrl);
    }
}
