package com.example.hack1.controller;

import com.oreo.insight.domain.Role;
import com.oreo.insight.domain.Sale;
import com.oreo.insight.domain.User;
import com.oreo.insight.dto.SaleRequest;
import com.oreo.insight.dto.SaleResponse;
import com.oreo.insight.repository.UserRepository;
import com.oreo.insight.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;
    private final UserRepository userRepository;
    private final com.oreo.insight.service.WeeklySummaryService weeklySummaryService;

    public SalesController(SalesService salesService, UserRepository userRepository, com.oreo.insight.service.WeeklySummaryService weeklySummaryService) {
        this.salesService = salesService;
        this.userRepository = userRepository;
        this.weeklySummaryService = weeklySummaryService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody SaleRequest req, Authentication auth) {
        User current = getCurrentUser(auth);
        try {
            Sale saved = salesService.create(req, current);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (SalesService.ForbiddenBranchException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("FORBIDDEN", "branch not allowed for BRANCH user"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id, Authentication auth) {
        User current = getCurrentUser(auth);
        Optional<Sale> opt = salesService.getById(id, current);
        if (opt.isEmpty()) {
            if (current.getRole() == Role.BRANCH) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("FORBIDDEN", "cannot access sale from another branch"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", "sale not found"));
        }
        return ResponseEntity.ok(toResponse(opt.get()));
    }

    @GetMapping
    public ResponseEntity<Page<SaleResponse>> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        User current = getCurrentUser(auth);
        Pageable pageable = PageRequest.of(page, size);
        Page<Sale> sales = salesService.list(from, to, branch, pageable, current);
        Page<SaleResponse> mapped = sales.map(this::toResponse);
        return ResponseEntity.ok(mapped);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody SaleRequest req, Authentication auth) {
        User current = getCurrentUser(auth);
        Optional<Sale> updated = salesService.update(id, req, current);
        if (updated.isEmpty()) {
            if (current.getRole() == Role.BRANCH) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("FORBIDDEN", "cannot modify sale from another branch"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", "sale not found"));
        }
        return ResponseEntity.ok(toResponse(updated.get()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CENTRAL')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        boolean existed = salesService.delete(id);
        if (!existed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", "sale not found"));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/summary/weekly")
    @PreAuthorize("hasAnyRole('CENTRAL','BRANCH')")
    public ResponseEntity<?> requestWeeklySummary(@Valid @RequestBody com.oreo.insight.dto.WeeklySummaryRequest req, Authentication auth) {
        // Calcular semana por defecto si no se envían fechas
        java.time.LocalDate to = req.getTo() != null ? req.getTo() : java.time.LocalDate.now();
        java.time.LocalDate from = req.getFrom() != null ? req.getFrom() : to.minusDays(7);
        
        // Enfoque de permisos:
        // - CENTRAL: puede omitir branch o indicar cualquiera.
        // - BRANCH: si omite branch, se usa su branch; si indica otro, se rechaza.
        User current = getCurrentUser(auth);
        String branch = req.getBranch();
        if (current.getRole() == Role.BRANCH) {
            if (branch == null || branch.isBlank()) {
                branch = current.getBranch();
            } else if (!branch.equals(current.getBranch())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("FORBIDDEN", "cannot request summary for another branch"));
            }
        }
        
        String requestId = weeklySummaryService.enqueueSummary(req);
        
        SummaryAccepted ack = new SummaryAccepted(
                requestId,
                "PROCESSING",
                "Resumen semanal en proceso. Se enviará a: " + req.getEmailTo(),
                "30-60 segundos",
                Instant.now()
        );
        return ResponseEntity.accepted().body(ack);
    }

    @PostMapping("/summary/weekly/premium")
    @PreAuthorize("hasAnyRole('CENTRAL','BRANCH')")
    public ResponseEntity<?> requestWeeklySummaryPremium(@Valid @RequestBody com.oreo.insight.dto.WeeklyPremiumRequest req, Authentication auth) {
        // Calcular semana por defecto si no se envían fechas
        java.time.LocalDate to = req.getTo() != null ? req.getTo() : java.time.LocalDate.now();
        java.time.LocalDate from = req.getFrom() != null ? req.getFrom() : to.minusDays(7);
        
        // Permisos por rol
        User current = getCurrentUser(auth);
        String branch = req.getBranch();
        if (current.getRole() == Role.BRANCH) {
            if (branch == null || branch.isBlank()) {
                branch = current.getBranch();
                req.setBranch(branch);
            } else if (!branch.equals(current.getBranch())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("FORBIDDEN", "cannot request summary for another branch"));
            }
        }
        
        String requestId = weeklySummaryService.enqueuePremiumSummary(req);
        String featuresMessage = "Reporte premium en proceso. " +
                (req.isIncludeCharts() ? "Incluirá gráficos. " : "") +
                (req.isAttachPdf() ? "Adjuntará PDF." : "");
        
        SummaryAccepted ack = new SummaryAccepted(
                requestId,
                "PROCESSING",
                featuresMessage + " Se enviará a: " + req.getEmailTo(),
                "60-90 segundos",
                Instant.now()
        );
        return ResponseEntity.accepted().body(ack);
    }
    private User getCurrentUser(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    public record SummaryAccepted(String requestId, String status, String message, String estimatedTime, Instant requestedAt) {}

    private SaleResponse toResponse(Sale s) {
        return new SaleResponse(
                s.getId(),
                s.getSku(),
                s.getUnits(),
                s.getPrice().doubleValue(),
                s.getBranch(),
                s.getSoldAt(),
                s.getCreatedBy()
        );
    }

    private ErrorBody error(String code, String message) {
        return new ErrorBody(code, message);
    }

    public record ErrorBody(String error, String message) {}
}