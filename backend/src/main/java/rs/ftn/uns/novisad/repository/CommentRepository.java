package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.ftn.uns.novisad.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Komentar samog utiska (bez roditelja); [M3] odgovori dolaze kasnije. */
    @EntityGraph(attributePaths = "author")
    Optional<Comment> findFirstByReviewIdAndParentIsNullAndActiveTrueOrderByCreatedAtAsc(Long reviewId);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByReviewIdAndActiveTrueOrderByCreatedAtAsc(Long reviewId);
}
