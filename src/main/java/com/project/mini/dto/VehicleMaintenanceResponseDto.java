package com.project.mini.dto;

import com.project.mini.entity.VehicleMaintenance;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class VehicleMaintenanceResponseDto {
    private Long id;
    private Long vehicleId;
    private String maintenanceType;
    private String description;
    private BigDecimal cost;
    private LocalDate maintenanceDate;
    private LocalDate nextDueDate;
    private LocalDateTime createdAt;

    public VehicleMaintenanceResponseDto(VehicleMaintenance maintenance) {
        this.id = maintenance.getId();
        this.vehicleId = maintenance.getVehicle().getId();
        this.maintenanceType = maintenance.getMaintenanceType();
        this.description = maintenance.getDescription();
        this.cost = maintenance.getCost();
        this.maintenanceDate = maintenance.getMaintenanceDate();
        this.nextDueDate = maintenance.getNextDueDate();
        this.createdAt = maintenance.getCreatedAt();
    }
}