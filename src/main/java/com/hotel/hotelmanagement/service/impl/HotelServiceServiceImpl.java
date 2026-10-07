package com.hotel.hotelmanagement.service.impl;

import com.hotel.hotelmanagement.entity.HotelService;
import com.hotel.hotelmanagement.repository.HotelServiceRepository;
import com.hotel.hotelmanagement.service.HotelServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HotelServiceServiceImpl implements HotelServiceService {

    private final HotelServiceRepository repository;

    public HotelServiceServiceImpl(HotelServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HotelService> findAll() {
        return repository.findAll();
    }

    @Override
    public Page<HotelService> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<HotelService> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public HotelService save(HotelService service) {
        return repository.save(service);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        repository.findById(id).ifPresent(s -> {
            s.setActive(!s.getActive());
            repository.save(s);
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

    private org.springframework.data.jpa.domain.Specification<HotelService> createServiceSpec(String name, String status) {
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
    public List<HotelService> searchServices(String name, String status) {
        return repository.findAll(createServiceSpec(name, status));
    }

    @Override
    public Page<HotelService> searchServices(String name, String status, Pageable pageable) {
        return repository.findAll(createServiceSpec(name, status), pageable);
    }
}
