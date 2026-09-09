package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ftn.uns.novisad.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
