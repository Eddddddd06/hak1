package com.example.hack1.service;

import com.oreo.insight.domain.Role;
import com.oreo.insight.domain.Sale;
import com.oreo.insight.domain.User;
import com.oreo.insight.dto.SaleRequest;
import com.oreo.insight.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class SalesService {

    private final SaleRepository saleRepository;

    public SalesService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public Sale create(SaleRequest req, User currentUser) {
        String effectiveBranch = resolveBranchForCreate(req.getBranch(), currentUser);
        Sale sale = new Sale();
        sale.setSku(req.getSku());
        sale.setUnits(req.getUnits());
        sale.setPrice(BigDecimal.valueOf(req.getPrice()));
        sale.setBranch(effectiveBranch);
        sale.setSoldAt(req.getSoldAt() != null ? req.getSoldAt() : Instant.now());
        sale.setCreatedBy(currentUser.getUsername());
        return saleRepository.save(sale);
    }

    public Optional<Sale> getById(String id, User currentUser) {
        Optional<Sale> opt = saleRepository.findById(id);
        if (opt.isEmpty()) return Optional.empty();
        Sale sale = opt.get();
        if (currentUser.getRole() == Role.BRANCH && !sale.getBranch().equals(currentUser.getBranch())) {
            return Optional.empty();
        }
        return Optional.of(sale);
    }

    public Page<Sale> list(Instant from, Instant to, String branch, Pageable pageable, User currentUser) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now().plusSeconds(1);
        String effectiveBranch = branch;
        if (currentUser.getRole() == Role.BRANCH) {
            effectiveBranch = currentUser.getBranch();
        }
        return saleRepository.findByFilters(effectiveFrom, effectiveTo, effectiveBranch, pageable);
    }

    public Optional<Sale> update(String id, SaleRequest req, User currentUser) {
        Optional<Sale> opt = saleRepository.findById(id);
        if (opt.isEmpty()) return Optional.empty();
        Sale sale = opt.get();
        if (currentUser.getRole() == Role.BRANCH && !sale.getBranch().equals(currentUser.getBranch())) {
            // no pertenece a su sucursal
            return Optional.empty();
        }
        sale.setSku(req.getSku());
        sale.setUnits(req.getUnits());
        sale.setPrice(BigDecimal.valueOf(req.getPrice()));
        if (req.getSoldAt() != null) {
            sale.setSoldAt(req.getSoldAt());
        }
        if (currentUser.getRole() == Role.CENTRAL && req.getBranch() != null && !req.getBranch().isBlank()) {
            sale.setBranch(req.getBranch());
        } else if (currentUser.getRole() == Role.BRANCH && req.getBranch() != null && !req.getBranch().equals(currentUser.getBranch())) {
            // intento de cambiar a otra sucursal -> prohibido; retornamos vacío para que el controller responda 403
            return Optional.empty();
        }
        return Optional.of(saleRepository.save(sale));
    }

    public boolean delete(String id) {
        if (saleRepository.existsById(id)) {
            saleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private String resolveBranchForCreate(String requestedBranch, User currentUser) {
        if (currentUser.getRole() == Role.CENTRAL) {
            return requestedBranch;
        }
        // BRANCH: debe ser su propia sucursal
        if (requestedBranch != null && !requestedBranch.equals(currentUser.getBranch())) {
            throw new ForbiddenBranchException();
        }
        return currentUser.getBranch();
    }

    public static class ForbiddenBranchException extends RuntimeException {}
}