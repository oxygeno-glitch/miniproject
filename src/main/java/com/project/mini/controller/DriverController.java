package com.project.mini.controller;

import com.project.mini.dto.DriverRequestDto;
import com.project.mini.dto.DriverResponseDto;
import com.project.mini.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    /**
     * 운전자 신규 등록 API
     * POST /api/drivers
     */
    @PostMapping
    public ResponseEntity<DriverResponseDto> registerDriver(@RequestBody DriverRequestDto requestDto) {
        DriverResponseDto responseDto = driverService.registerDriver(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 전체 운전자 목록 조회 API
     * GET /api/drivers
     */
    @GetMapping
    public ResponseEntity<List<DriverResponseDto>> getAllDrivers() {
        List<DriverResponseDto> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }

    /**
     * 특정 운전자 단건 조회 API
     * GET /api/drivers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDto> getDriverById(@PathVariable Long id) {
        DriverResponseDto driver = driverService.getDriverById(id);
        return ResponseEntity.ok(driver);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponseDto> updateWorkStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request) {
        String workStatus = request.get("workStatus");
        DriverResponseDto updatedDriver = driverService.updateWorkStatus(id, workStatus);
        return ResponseEntity.ok(updatedDriver);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}