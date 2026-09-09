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
 * [K5] Utisak koji korisnik ostavlja na mesto.
 * <p>
 * Utisak se vezuje za dogadjaj koji se odrzao na tom mestu; uz njega idu ocene
 * ({@link Rate}) i opciono komentar ({@link Comment}).
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_author_event",
                columnNames = {"author_id", "event_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /** Dogadjaj koji se odrzao na mestu i na koji se utisak odnosi. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rate> rates = new ArrayList<>();

    /**
     * Koliko se puta dogadjaj ukupno odrzao u trenutku pisanja utiska.
     * Cuva se kao snimak jer specifikacija trazi bas tu vrednost, a broj
     * odrzavanja kasnije raste.
     */
    @Column(name = "times_held_at_review", nullable = false)
    private long timesHeldAtReview;

    /**
     * [M2] Sakriven utisak se ne prikazuje, ali se njegove ocene i dalje
     * racunaju u ukupnoj oceni mesta.
     */
    @Column(nullable = false)
    private boolean hidden;

    /**
     * [M2] Uklonjen utisak se logicki brise i njegove ocene se ponistavaju
     * (ne ulaze u ukupnu ocenu mesta).
     */
    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
