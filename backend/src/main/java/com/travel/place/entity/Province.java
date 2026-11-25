package com.travel.place.entity;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "provinces")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Province province = (Province) o;
        return id != null && Objects.equals(id, province.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
