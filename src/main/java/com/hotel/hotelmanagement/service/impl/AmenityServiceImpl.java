package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.Amenity;
import com.hotel.hotelmanagement.repository.AmenityRepository;
import com.hotel.hotelmanagement.service.AmenityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository repository;

    public AmenityServiceImpl(AmenityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Amenity> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<Amenity> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<Amenity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public Amenity save(Amenity amenity) {
        return repository.save(amenity);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        repository.findById(id).ifPresent(a -> {
            a.setActive(!a.getActive());
            repository.save(a);
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

    private org.springframework.data.jpa.domain.Specification<Amenity> createAmenitySpec(String name, String status) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            if (org.springframework.util.StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (org.springframework.util.StringUtils.hasText(status)) {
                if ("active".equalsIgnoreCase(status)) {
                    predicates.add(cb.isTrue(root.get("active")));
                } else if ("inactive".equalsIgnoreCase(status)) {
                    predicates.add(cb.isFalse(root.get("active")));
                }
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    public List<Amenity> searchAmenities(String name, String status) {
        return repository.findAll(createAmenitySpec(name, status));
    }

    @Override
    public Page<Amenity> searchAmenities(String name, String status, Pageable pageable) {
        return repository.findAll(createAmenitySpec(name, status), pageable);
    }
}
