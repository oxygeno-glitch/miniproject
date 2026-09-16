package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VehicleMaintenanceRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        private Long vehicleId;
        private String maintenanceType; // INSPECTION, REPAIR
        private String description;
        private BigDecimal cost;
        private LocalDate maintenanceDate;
        private LocalDate nextDueDate;
    }
}