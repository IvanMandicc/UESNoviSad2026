package rs.ftn.uns.novisad.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * [K3] Mesto na kojem se odrzavaju dogadjaji.
 * Obavezna polja: naziv, adresa, tip mesta, opis i slika.
 */
@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LocationType type;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Kljuc slike u skladistu (vidi StorageService). Slika je obavezna. */
    @Column(name = "image_key", nullable = false, length = 300)
    private String imageKey;

    /**
     * Logicko brisanje. Vecinu informacionih sistema karakterise neogranicen
     * period cuvanja podataka, pa se mesto ne brise fizicki.
     */
    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Manages> managers = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
