package com.project.mini.controller;

import com.project.mini.dto.VehicleRequestDto;
import com.project.mini.dto.VehicleResponseDto;
import com.project.mini.dto.VehicleStatusUpdateRequestDto;
import com.project.mini.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * 차량 신규 등록 API
     * POST /api/vehicles
     */
    @PostMapping
    public ResponseEntity<VehicleResponseDto> registerVehicle(
            @RequestBody VehicleRequestDto requestDto
    ) {
        VehicleResponseDto responseDto =
                vehicleService.registerVehicle(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDto);
    }

    /**
     * 전체 차량 목록 조회 API
     * GET /api/vehicles
     */
    @GetMapping
    public ResponseEntity<List<VehicleResponseDto>> getAllVehicles() {
        List<VehicleResponseDto> vehicles =
                vehicleService.getAllVehicles();

        return ResponseEntity.ok(vehicles);
    }

    /**
     * 특정 차량 단건 조회 API
     * GET /api/vehicles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDto> getVehicleById(
            @PathVariable Long id
    ) {
        VehicleResponseDto vehicle =
                vehicleService.getVehicleById(id);

        return ResponseEntity.ok(vehicle);
    }

    /**
     * 차량 상태 변경 API
     * PATCH /api/vehicles/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody VehicleStatusUpdateRequestDto requestDto
    ) {
        VehicleResponseDto vehicle =
                vehicleService.updateStatus(id, requestDto);

        return ResponseEntity.ok(vehicle);
    }

    /**
     * 차량 삭제 API
     * DELETE /api/vehicles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable Long id
    ) {
        vehicleService.deleteVehicle(id);

        return ResponseEntity.noContent().build();
    }
}