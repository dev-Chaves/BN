package com.bn.benefix.manager;

import com.bn.benefix.manager.dto.ManagerCreationRequestDTO;
import com.bn.benefix.manager.dto.ManagerCreationResponseDTO;
import com.bn.benefix.manager.dto.ManagerUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("manager")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManagerCreationResponseDTO> createManager(
            @Valid @RequestBody ManagerCreationRequestDTO dto,
            UriComponentsBuilder builder){

        ManagerCreationResponseDTO response = managerService.createManager(dto);

        URI uri = builder.path("/manager/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ManagerCreationResponseDTO>> getAllManagers() {
        return ResponseEntity.ok(managerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagerCreationResponseDTO> getManagerById(@PathVariable Long id) {
        return ResponseEntity.ok(managerService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManagerCreationResponseDTO> updateManager(
            @PathVariable Long id,
            @RequestBody ManagerUpdateRequestDTO dto) {
        return ResponseEntity.ok(managerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
