package com.travel.place.repository;

import com.travel.place.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, UUID> {

    Optional<Province> findByCode(String code);

    List<Province> findByNameContainingIgnoreCase(String name);

    boolean existsByCode(String code);
}
