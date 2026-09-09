package rs.ftn.uns.novisad.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ftn.uns.novisad.dto.UserDto;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.repository.UserRepository;

import java.util.List;

/**
 * [A2] Spisak korisnika iz kog administrator bira kandidata za menadzera.
 * Administratori se izostavljaju jer ne mogu biti menadzeri mesta.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> list() {
        List<UserDto> users = userRepository.findAll().stream()
                .filter(user -> user.getRole() != Role.ADMIN)
                .filter(rs.ftn.uns.novisad.model.User::isEnabled)
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
