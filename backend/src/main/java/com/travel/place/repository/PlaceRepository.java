package com.travel.place.repository;

import com.travel.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    List<Place> findAllByIdIn(Collection<UUID> ids);

    List<Place> findByProvinceId(UUID provinceId);

    List<Place> findByNameContainingIgnoreCase(String name);

    Optional<Place> findByProviderPlaceId(String providerPlaceId);
}
