package rs.ftn.uns.novisad.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [K5] Jedna ocena u okviru utiska - po jednoj stavci, na skali 1-10.
 * Postoji samo za stavke koje je korisnik zaista ocenio.
 */
@Entity
@Table(
        name = "rates",
        uniqueConstraints = @UniqueConstraint(name = "uk_rate_review_category",
                columnNames = {"review_id", "category"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RateCategory category;

    /**
     * Ocena na skali od 1 do 10.
     * Kolona se zove "rate_value" jer je VALUE rezervisana rec u SQL-u.
     */
    @Column(name = "rate_value", nullable = false)
    private int value;
}
