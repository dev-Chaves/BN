package com.bn.benefix.management;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("management")
public class ManagementController {

    private final ManagerService managerService;

    public ManagementController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @org.springframework.web.bind.annotation.PostMapping()
    public org.springframework.http.ResponseEntity<com.bn.benefix.management.dto.ManagerCreationResponseDTO> createManager(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.bn.benefix.management.dto.ManagerCreationRequestDTO dto,
            org.springframework.web.util.UriComponentsBuilder builder){

        com.bn.benefix.management.dto.ManagerCreationResponseDTO response = managerService.createManager(dto);

        java.net.URI uri = builder.path("/management/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return org.springframework.http.ResponseEntity.created(uri).body(response);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public org.springframework.http.ResponseEntity<java.util.List<com.bn.benefix.management.dto.ManagerCreationResponseDTO>> getAllManagers() {
        return org.springframework.http.ResponseEntity.ok(managerService.findAll());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public org.springframework.http.ResponseEntity<com.bn.benefix.management.dto.ManagerCreationResponseDTO> getManagerById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok(managerService.findById(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public org.springframework.http.ResponseEntity<com.bn.benefix.management.dto.ManagerCreationResponseDTO> updateManager(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody com.bn.benefix.management.dto.ManagerUpdateRequestDTO dto) {
        return org.springframework.http.ResponseEntity.ok(managerService.update(id, dto));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> deleteManager(@org.springframework.web.bind.annotation.PathVariable Long id) {
        managerService.delete(id);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

}
