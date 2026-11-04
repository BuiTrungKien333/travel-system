package com.travel.trip.entity;

import com.travel.common.persistence.BaseEntity;
import com.travel.trip.enums.TripStatus;
import com.travel.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "trips")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "destination")
    private String destination;

    @Column(name = "transportation", length = 50)
    private String transportation;

    @Column(name = "budget", precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 2048)
    private String coverImage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences;

    @Column(name = "constraints", columnDefinition = "TEXT")
    private String constraints;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.PLANNING;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "time_zone", nullable = false, length = 64)
    @Builder.Default
    private String timeZone = "Asia/Ho_Chi_Minh";

    @Column(name = "destination_province_id")
    private java.util.UUID destinationProvinceId;

    @Column(name = "budget_amount")
    private Long budgetAmount;

    @Column(name = "budget_currency", length = 3)
    private String budgetCurrency;

    @Column(length = 200)
    private String accommodation;

    @Column(length = 1000)
    private String notes;

    @Column(name = "client_request_id")
    private java.util.UUID clientRequestId;

    @Column(name = "create_request_hash", length = 64)
    private String createRequestHash;
}
