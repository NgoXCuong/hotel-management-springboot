package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.RoomType;
import com.hotel.hotelmanagement.entity.RoomTypeImage;
import com.hotel.hotelmanagement.repository.RoomTypeRepository;
import com.hotel.hotelmanagement.service.CloudinaryService;
import com.hotel.hotelmanagement.service.RoomTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository repository;
    private final CloudinaryService cloudinaryService;

    public RoomTypeServiceImpl(RoomTypeRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public List<RoomType> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<RoomType> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<RoomType> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public RoomType save(RoomType roomType) {
        return save(roomType, null, null);
    }

    @Override
    @Transactional
    public RoomType save(RoomType roomType, List<String> newUploadedImageUrls, List<String> deletedImageUrls) {
        RoomType entityToSave = roomType;

        if (roomType.getId() != null) {
            Optional<RoomType> existingOpt = repository.findById(roomType.getId());
            if (existingOpt.isPresent()) {
                RoomType existing = existingOpt.get();
                existing.setName(roomType.getName());
                existing.setDescription(roomType.getDescription());
                existing.setPricePerNight(roomType.getPricePerNight());
                existing.setMaxGuests(roomType.getMaxGuests());
                existing.setActive(roomType.getActive());
                existing.setAmenities(roomType.getAmenities());

                // Xóa các ảnh được đánh dấu xóa
                if (deletedImageUrls != null && !deletedImageUrls.isEmpty()) {
                    List<RoomTypeImage> imagesToRemove = new ArrayList<>();
                    for (RoomTypeImage img : existing.getImages()) {
                        if (deletedImageUrls.contains(img.getImageUrl())) {
                            imagesToRemove.add(img);
                            cloudinaryService.deleteImageByUrl(img.getImageUrl());
                        }
                    }
                    existing.getImages().removeAll(imagesToRemove);
                }

                entityToSave = existing;
            }
        } else {
            if (entityToSave.getImages() == null) {
                entityToSave.setImages(new ArrayList<>());
            }
        }

        // Thêm các ảnh mới được upload lên Cloudinary
        if (newUploadedImageUrls != null && !newUploadedImageUrls.isEmpty()) {
            for (String url : newUploadedImageUrls) {
                if (url != null && !url.isBlank()) {
                    RoomTypeImage img = new RoomTypeImage();
                    img.setImageUrl(url.trim());
                    img.setRoomType(entityToSave);
                    img.setIsPrimary(false);
                    img.setDisplayOrder(entityToSave.getImages().size());
                    entityToSave.getImages().add(img);
                }
            }
        }

        // Cập nhật lại thứ tự hiển thị và ảnh đại diện chính (ảnh đầu tiên là Primary)
        if (entityToSave.getImages() != null && !entityToSave.getImages().isEmpty()) {
            for (int i = 0; i < entityToSave.getImages().size(); i++) {
                RoomTypeImage img = entityToSave.getImages().get(i);
                img.setDisplayOrder(i);
                img.setIsPrimary(i == 0);
            }
        }

        return repository.save(entityToSave);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        repository.findById(id).ifPresent(rt -> {
            rt.setActive(!rt.getActive());
            repository.save(rt);
        });
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return repository.existsByNameAndIdNot(name, id);
    }
}
