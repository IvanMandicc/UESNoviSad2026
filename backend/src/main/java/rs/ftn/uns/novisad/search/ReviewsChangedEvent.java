package rs.ftn.uns.novisad.search;

/**
 * [UES] Javlja da su se utisci nekog mesta promenili, pa indeks vise ne odgovara
 * stanju u bazi (broj utisaka i prosecne ocene).
 * <p>
 * Ide preko dogadjaja, a ne direktnim pozivom, jer bi ReviewService i
 * LocationIndexService inace zavisili jedan od drugog u krug.
 */
public record ReviewsChangedEvent(Long locationId) {
}
