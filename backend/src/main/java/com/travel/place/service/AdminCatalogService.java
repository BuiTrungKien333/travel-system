package com.travel.place.service;

import com.travel.common.exception.*;
import com.travel.security.util.SecurityUtils;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:catalog')")
public class AdminCatalogService {
    private final JdbcTemplate jdbc;
    public record Entry(UUID id, String name, String sourceUrl, String verificationScope, boolean approved, int version) { }
    public record Page(List<Entry> content, int page, int size, long totalElements) { }
    public record Review(@NotNull Boolean approved, @NotNull @Positive Integer version) { }
    private static final String JOIN = " FROM places p JOIN place_retrieval_profiles r ON r.place_id=p.id WHERE p.source='INTERNAL'";
    @Transactional(readOnly = true)
    public Page list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new AppException(ErrorCode.VALIDATION_ERROR);
        return new Page(jdbc.query("SELECT p.id,p.name,r.*" + JOIN + " ORDER BY p.name,p.id LIMIT ? OFFSET ?", (rs,n) -> new Entry(
                rs.getObject("id", UUID.class),rs.getString("name"),rs.getString("source_url"),rs.getString("verification_scope"),
                rs.getBoolean("approved"),rs.getInt("content_version")), size,(long)page*size),page,size,
                jdbc.queryForObject("SELECT count(*)" + JOIN, Long.class));
    }
    @Transactional
    public void review(UUID id, Review request) {
        var entries = jdbc.query("SELECT p.id,p.name,r.*" + JOIN + " AND p.id=? FOR UPDATE OF r", (rs,n) -> new Entry(
                rs.getObject("id", UUID.class),rs.getString("name"),rs.getString("source_url"),rs.getString("verification_scope"),
                rs.getBoolean("approved"),rs.getInt("content_version")),id);
        if (entries.isEmpty()) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        var entry = entries.getFirst();
        if (entry.version() != request.version()) throw new AppException(ErrorCode.ADMIN_ACCOUNT_CONFLICT);
        if (request.approved() && entry.verificationScope().equals("DEV_DEMO_ONLY_NOT_FACT_VERIFIED"))
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        if (entry.approved() == request.approved()) return;
        jdbc.update("UPDATE place_retrieval_profiles SET approved=?,content_version=content_version+1 WHERE place_id=?", request.approved(),id);
        jdbc.update("INSERT INTO admin_catalog_events(actor_id,place_id,was_approved,is_approved) VALUES (?,?,?,?)",
                SecurityUtils.getCurrentUserId(),id,entry.approved(),request.approved());
    }
}
