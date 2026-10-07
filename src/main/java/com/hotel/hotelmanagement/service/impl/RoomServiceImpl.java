package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.Room;
import com.hotel.hotelmanagement.repository.RoomRepository;
import com.hotel.hotelmanagement.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository repository;

    public RoomServiceImpl(RoomRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Room> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Room> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public Room save(Room room) {
        return repository.save(room);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        repository.findById(id).ifPresent(room -> {
            room.setActive(!room.getActive());
            repository.save(room);
        });
    }

    @Override
    public boolean existsByRoomNumber(String roomNumber) {
        return repository.existsByRoomNumber(roomNumber);
    }

    @Override
    public boolean existsByRoomNumberAndIdNot(String roomNumber, Long id) {
        return repository.existsByRoomNumberAndIdNot(roomNumber, id);
    }

    private org.springframework.data.jpa.domain.Specification<Room> createRoomSpec(String roomNumber, Integer floor, Long roomTypeId, String status) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            if (org.springframework.util.StringUtils.hasText(roomNumber)) {
                predicates.add(cb.like(cb.lower(root.get("roomNumber")), "%" + roomNumber.toLowerCase() + "%"));
            }
            if (floor != null) {
                predicates.add(cb.equal(root.get("floor"), floor));
            }
            if (roomTypeId != null) {
                predicates.add(cb.equal(root.get("roomType").get("id"), roomTypeId));
            }
            if (org.springframework.util.StringUtils.hasText(status)) {
                if ("active".equalsIgnoreCase(status)) {
                    predicates.add(cb.isTrue(root.get("active")));
                } else if ("inactive".equalsIgnoreCase(status)) {
                    predicates.add(cb.isFalse(root.get("active")));
                } else {
                    try {
                        Room.RoomStatus roomStatus = Room.RoomStatus.valueOf(status.toUpperCase());
                        predicates.add(cb.equal(root.get("status"), roomStatus));
                    } catch (IllegalArgumentException e) {
                        // ignore invalid status
                    }
                }
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    public List<Room> searchRooms(String roomNumber, Integer floor, Long roomTypeId, String status) {
        return repository.findAll(createRoomSpec(roomNumber, floor, roomTypeId, status));
    }

    @Override
    public org.springframework.data.domain.Page<Room> searchRooms(String roomNumber, Integer floor, Long roomTypeId, String status, org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(createRoomSpec(roomNumber, floor, roomTypeId, status), pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Room> findAll(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Integer> findDistinctFloors() {
        return repository.findDistinctFloors();
    }
}
