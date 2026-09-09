package rs.ftn.uns.novisad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ftn.uns.novisad.model.AccountRequest;
import rs.ftn.uns.novisad.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    List<AccountRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    List<AccountRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByEmailIgnoreCaseAndStatus(String email, RequestStatus status);

    Optional<AccountRequest> findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
