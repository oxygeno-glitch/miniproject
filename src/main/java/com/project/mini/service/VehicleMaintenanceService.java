package com.project.mini.service;

import com.project.mini.dto.VehicleMaintenanceRequestDto;
import com.project.mini.dto.VehicleMaintenanceResponseDto;
import com.project.mini.entity.Vehicle;
import com.project.mini.entity.VehicleMaintenance;
import com.project.mini.repository.VehicleMaintenanceRepository;
import com.project.mini.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleMaintenanceService {

    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * 정비 이력 등록
     */
    @Transactional
    public VehicleMaintenanceResponseDto createMaintenance(VehicleMaintenanceRequestDto.Create requestDto) {
        Vehicle vehicle = vehicleRepository.findById(requestDto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 차량 ID: " + requestDto.getVehicleId()));

        VehicleMaintenance maintenance = VehicleMaintenance.builder()
                .vehicle(vehicle)
                .maintenanceType(requestDto.getMaintenanceType())
                .description(requestDto.getDescription())
                .cost(requestDto.getCost() != null ? requestDto.getCost() : BigDecimal.ZERO)
                .maintenanceDate(requestDto.getMaintenanceDate())
                .nextDueDate(requestDto.getNextDueDate())
                .build();

        return new VehicleMaintenanceResponseDto(vehicleMaintenanceRepository.save(maintenance));
    }

    /**
     * 전체 정비 이력 목록 조회
     */
    public List<VehicleMaintenanceResponseDto> getAllMaintenances() {
        return vehicleMaintenanceRepository.findAll().stream()
                .map(VehicleMaintenanceResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 특정 차량의 정비 이력 조회
     */
    public List<VehicleMaintenanceResponseDto> getMaintenancesByVehicleId(Long vehicleId) {
        return vehicleMaintenanceRepository.findAll().stream()
                .filter(m -> m.getVehicle().getId().equals(vehicleId))
                .map(VehicleMaintenanceResponseDto::new)
                .collect(Collectors.toList());
    }
}