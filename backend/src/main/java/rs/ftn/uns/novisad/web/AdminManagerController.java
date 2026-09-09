package rs.ftn.uns.novisad.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ftn.uns.novisad.dto.AssignManagerDto;
import rs.ftn.uns.novisad.dto.ManagerDto;
import rs.ftn.uns.novisad.service.ManagerService;

import java.util.List;

/** [A2] Dodavanje i uklanjanje menadzera na mestu - samo administrator. */
@RestController
@RequestMapping("/api/admin/locations/{locationId}/managers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagerController {

    private final ManagerService managerService;

    public AdminManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public ResponseEntity<List<ManagerDto>> list(@PathVariable Long locationId) {
        return ResponseEntity.ok(managerService.findByLocation(locationId).stream().map(ManagerDto::from).toList());
    }

    @PostMapping
    public ResponseEntity<ManagerDto> assign(@PathVariable Long locationId,
                                             @Valid @RequestBody AssignManagerDto dto) {
        ManagerDto assigned = ManagerDto.from(managerService.assign(locationId, dto.userId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable Long locationId, @PathVariable Long userId) {
        managerService.remove(locationId, userId);
        return ResponseEntity.noContent().build();
    }
}
