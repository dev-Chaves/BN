package com.bnfix.ubm.domains.enrollment;

import com.bnfix.ubm.domains.enrollment.dto.EnrollmentRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/companies/event")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final long eventCompanyId;

    public EnrollmentController(
            EnrollmentService enrollmentService, @Value("${app.event.company-id:0}") long eventCompanyId) {
        this.enrollmentService = enrollmentService;
        this.eventCompanyId = eventCompanyId;
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@Valid @RequestBody EnrollmentRequest request) {
        log.info("Event enrollment attempt for email {}", request.email());
        return ResponseEntity.status(201).body(enrollmentService.enroll(request, eventCompanyId));
    }
}
