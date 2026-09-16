package com.project.mini.controller;

import com.project.mini.dto.VehicleMaintenanceRequestDto;
import com.project.mini.dto.VehicleMaintenanceResponseDto;
import com.project.mini.service.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
public class VehicleMaintenanceController {

    private final VehicleMaintenanceService vehicleMaintenanceService;

    @PostMapping
    public ResponseEntity<VehicleMaintenanceResponseDto> createMaintenance(
            @RequestBody VehicleMaintenanceRequestDto.Create requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleMaintenanceService.createMaintenance(requestDto));
    }

    @GetMapping
    public ResponseEntity<List<VehicleMaintenanceResponseDto>> getAllMaintenances() {
        return ResponseEntity.ok(vehicleMaintenanceService.getAllMaintenances());
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleMaintenanceResponseDto>> getMaintenancesByVehicleId(
            @PathVariable Long vehicleId) {
        return ResponseEntity.ok(vehicleMaintenanceService.getMaintenancesByVehicleId(vehicleId));
    }
}