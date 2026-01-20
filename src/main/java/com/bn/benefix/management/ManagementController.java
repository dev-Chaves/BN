package com.bn.benefix.management;

import com.bn.benefix.management.dto.ManagerCreationRequestDTO;
import com.bn.benefix.management.dto.ManagerCreationResponseDTO;
import com.bn.benefix.management.dto.ManagerUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("management")
public class ManagementController {

    private final ManagerService managerService;

    public ManagementController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @PostMapping()
    public ResponseEntity<ManagerCreationResponseDTO> createManager(
            @Valid @RequestBody ManagerCreationRequestDTO dto,
            UriComponentsBuilder builder){

        ManagerCreationResponseDTO response = managerService.createManager(dto);

        URI uri = builder.path("/management/{id}")
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
    public ResponseEntity<ManagerCreationResponseDTO> updateManager(
            @PathVariable Long id,
            @RequestBody ManagerUpdateRequestDTO dto) {
        return ResponseEntity.ok(managerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
