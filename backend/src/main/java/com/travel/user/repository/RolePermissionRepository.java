package com.travel.user.repository;

import com.travel.user.entity.RolePermission;
import com.travel.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRole(Role role);

    @Query("SELECT rp.permission.name FROM RolePermission rp WHERE rp.role = :role")
    Set<String> findPermissionNamesByRole(@Param("role") Role role);
}
