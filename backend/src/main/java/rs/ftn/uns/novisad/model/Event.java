package rs.ftn.uns.novisad.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * [K4] Dogadjaj koji se odrzava na nekom mestu.
 * <p>
 * Obavezna polja po specifikaciji: naziv, mesto, adresa, tip, datum, oznaka da li je
 * dogadjaj redovan, cena ulaska ili informacija da je besplatan, i jedna slika.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * Adresa dogadjaja. Podrazumevano se preuzima sa mesta, ali je zasebno polje
     * jer je specifikacija navodi kao obavezan atribut dogadjaja.
     */
    @Column(nullable = false, length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType type;

    @Column(nullable = false)
    private LocalDateTime date;

    /**
     * Da li je dogadjaj redovan (ponavlja se). Utisak [K5] se moze ostaviti samo na
     * redovan dogadjaj koji se vec odrzao.
     */
    @Column(nullable = false)
    private boolean regular;

    /** Besplatan ulaz; kada je true, {@link #price} je null. */
    @Column(name = "free_entry", nullable = false)
    private boolean freeEntry;

    /** Cena ulaska; postavljena samo kada dogadjaj nije besplatan. */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_key", nullable = false, length = 300)
    private String imageKey;

    /** Logicko brisanje, u skladu sa napomenom o neogranicenom cuvanju podataka. */
    @Column(nullable = false)
    private boolean active;

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

    /** Da li se dogadjaj vec odrzao u odnosu na trenutno vreme. */
    @Transient
    public boolean hasTakenPlace() {
        return date.isBefore(LocalDateTime.now());
    }
}
