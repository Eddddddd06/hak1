package com.example.hack1.service;

import com.oreo.insight.domain.Sale;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SalesAggregationService {

    public AggregationResult aggregate(List<Sale> sales) {
        int totalUnits = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, Integer> unitsBySku = new HashMap<>();
        Map<String, BigDecimal> revenueBySku = new HashMap<>();
        Map<String, Integer> unitsByBranch = new HashMap<>();

        for (Sale s : sales) {
            totalUnits += s.getUnits();
            BigDecimal revenue = s.getPrice().multiply(BigDecimal.valueOf(s.getUnits()));
            totalRevenue = totalRevenue.add(revenue);

            unitsBySku.merge(s.getSku(), s.getUnits(), Integer::sum);
            revenueBySku.merge(s.getSku(), revenue, BigDecimal::add);
            unitsByBranch.merge(s.getBranch(), s.getUnits(), Integer::sum);
        }

        String topSkuByUnits = unitsBySku.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        String topBranchByUnits = unitsByBranch.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new AggregationResult(
                totalUnits,
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                unitsBySku,
                revenueBySku,
                unitsByBranch,
                topSkuByUnits,
                topBranchByUnits
        );
    }

    public record AggregationResult(
            int totalUnits,
            BigDecimal totalRevenue,
            Map<String, Integer> unitsBySku,
            Map<String, BigDecimal> revenueBySku,
            Map<String, Integer> unitsByBranch,
            String topSkuByUnits,
            String topBranchByUnits
    ) {}
}