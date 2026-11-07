package com.travel.itinerary.entity;

import com.travel.itinerary.dto.DaySettings;
import com.travel.itinerary.dto.DaySettings.TravelMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_day_settings", uniqueConstraints = @UniqueConstraint(columnNames = {"itinerary_id", "day_date"}))
@Getter
@NoArgsConstructor
public class ItineraryDaySettings {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;
    @Column(name = "day_date", nullable = false) private LocalDate date;
    @Column(name = "origin_name", length = 200) private String originName;
    @Column(name = "origin_address", length = 500) private String originAddress;
    private Double originLatitude;
    private Double originLongitude;
    @Column(nullable = false) private LocalTime departureTime;
    @Column(nullable = false) private boolean returnToStart;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TravelMode travelMode;

    public ItineraryDaySettings(Itinerary itinerary, LocalDate date) {
        this.itinerary = itinerary;
        this.date = date;
    }
    public void update(DaySettings settings) {
        var origin = settings.origin();
        originName = origin == null ? null : origin.name().trim();
        originAddress = origin == null || origin.address() == null ? null : origin.address().trim();
        originLatitude = origin == null ? null : origin.latitude();
        originLongitude = origin == null ? null : origin.longitude();
        departureTime = settings.departureTime();
        returnToStart = settings.returnToStart();
        travelMode = settings.travelMode();
    }
    public DaySettings snapshot() {
        return new DaySettings(originName == null ? null : new DaySettings.Origin(originName, originAddress,
                originLatitude, originLongitude), departureTime, returnToStart, travelMode);
    }
}
