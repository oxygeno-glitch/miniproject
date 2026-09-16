package com.project.mini.service;

import com.project.mini.dto.VehicleRequestDto;
import com.project.mini.dto.VehicleResponseDto;
import com.project.mini.entity.Vehicle;
import com.project.mini.dto.VehicleStatusUpdateRequestDto;
import com.project.mini.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * 차량 신규 등록
     */
    @Transactional
    public VehicleResponseDto registerVehicle(VehicleRequestDto requestDto) {
        // 번호판 중복 검증
        vehicleRepository.findByPlateNumber(requestDto.getPlateNumber())
                .ifPresent(v -> {
                    throw new IllegalArgumentException("이미 등록된 차량 번호입니다: " + requestDto.getPlateNumber());
                });

        Vehicle vehicle = Vehicle.builder()
                .plateNumber(requestDto.getPlateNumber())
                .modelName(requestDto.getModelName())
                .manufacturer(requestDto.getManufacturer())
                .modelYear(requestDto.getModelYear())
                .status("INACTIVE")
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return new VehicleResponseDto(savedVehicle);
    }

    /**
     * 전체 차량 목록 조회
     */
    public List<VehicleResponseDto> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 특정 차량 단건 조회 (ID 기준)
     */
    public VehicleResponseDto getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 차량이 존재하지 않습니다. ID: " + id));
        return new VehicleResponseDto(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 차량이 존재하지 않습니다. ID: " + id));

        vehicleRepository.delete(vehicle);
    }

    @Transactional
    public VehicleResponseDto updateStatus(
            Long id,
            VehicleStatusUpdateRequestDto requestDto
    ) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 차량이 존재하지 않습니다. ID: " + id
                        )
                );

        String status = requestDto.getStatus();

        if (!status.equals("ACTIVE")
                && !status.equals("MAINTENANCE")
                && !status.equals("INACTIVE")) {
            throw new IllegalArgumentException("올바르지 않은 차량 상태입니다.");
        }

        vehicle.updateStatus(status);

        return new VehicleResponseDto(vehicle);
    }
}