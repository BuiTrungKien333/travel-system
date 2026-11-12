package com.travel.place.repository;

import com.travel.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    @Query("select p from Place p where p.id=:id and (p.source='INTERNAL' or p.ownerId=:owner)")
    Optional<Place> findVisible(@Param("id") UUID id, @Param("owner") UUID owner);

    @EntityGraph(attributePaths = "province")
    @Query("select p from Place p where p.id in :ids and (p.source='INTERNAL' or p.ownerId=:owner)")
    List<Place> findVisibleByIds(@Param("ids") Collection<UUID> ids, @Param("owner") UUID owner);

    List<Place> findByIdInAndSource(Collection<UUID> ids, String source);

    List<Place> findAllByIdIn(Collection<UUID> ids);

}
