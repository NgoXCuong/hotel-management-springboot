package com.hotel.hotelmanagement.repository;

import com.hotel.hotelmanagement.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long>, JpaSpecificationExecutor<Amenity> {
    List<Amenity> findAllByActiveTrueOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    java.util.Optional<Amenity> findByName(String name);
}
