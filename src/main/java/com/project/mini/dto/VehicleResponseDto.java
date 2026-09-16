package com.project.mini.dto;

import com.project.mini.entity.Vehicle;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class VehicleResponseDto {
    private Long id;
    private String plateNumber;
    private String modelName;
    private String manufacturer;
    private Integer modelYear;
    private String status;
    private LocalDateTime createdAt;

    public VehicleResponseDto(Vehicle vehicle) {
        this.id = vehicle.getId();
        this.plateNumber = vehicle.getPlateNumber();
        this.modelName = vehicle.getModelName();
        this.manufacturer = vehicle.getManufacturer();
        this.modelYear = vehicle.getModelYear();
        this.status = vehicle.getStatus();
        this.createdAt = vehicle.getCreatedAt();
    }
}