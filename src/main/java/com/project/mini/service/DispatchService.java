package com.project.mini.service;

import com.project.mini.dto.DispatchRequestDto;
import com.project.mini.dto.DispatchResponseDto;
import com.project.mini.entity.Dispatch;
import com.project.mini.entity.Driver;
import com.project.mini.entity.Route;
import com.project.mini.entity.Vehicle;
import com.project.mini.repository.DispatchRepository;
import com.project.mini.repository.DriverRepository;
import com.project.mini.repository.RouteRepository;
import com.project.mini.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchService {

    private final DispatchRepository dispatchRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public DispatchResponseDto createDispatch(
            DispatchRequestDto.Create requestDto) {

        Route route = routeRepository.findById(requestDto.getRouteId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 노선 ID: "
                                        + requestDto.getRouteId()
                        )
                );

        Vehicle vehicle = vehicleRepository.findById(requestDto.getVehicleId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 차량 ID: "
                                        + requestDto.getVehicleId()
                        )
                );

        Driver driver = driverRepository.findById(requestDto.getDriverId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 기사 ID: "
                                        + requestDto.getDriverId()
                        )
                );

        if (!"INACTIVE".equals(vehicle.getStatus())) {
            throw new IllegalArgumentException(
                    "운행 대기 상태인 차량만 배차할 수 있습니다."
            );
        }

        if (!"STANDBY".equals(driver.getWorkStatus())) {
            throw new IllegalArgumentException(
                    "대기 상태인 기사만 배차할 수 있습니다."
            );
        }

        if (requestDto.getPlannedStartTime() == null
                || requestDto.getPlannedEndTime() == null) {
            throw new IllegalArgumentException(
                    "운행 예정 시간을 입력해주세요."
            );
        }

        if (!requestDto.getPlannedEndTime()
                .isAfter(requestDto.getPlannedStartTime())) {
            throw new IllegalArgumentException(
                    "예정 도착 시간은 예정 출발 시간보다 늦어야 합니다."
            );
        }

        Dispatch dispatch = Dispatch.builder()
                .route(route)
                .vehicle(vehicle)
                .driver(driver)
                .plannedStartTime(requestDto.getPlannedStartTime())
                .plannedEndTime(requestDto.getPlannedEndTime())
                .dispatchStatus("SCHEDULED")
                .build();

        return new DispatchResponseDto(
                dispatchRepository.save(dispatch)
        );
    }

    @Transactional
    public DispatchResponseDto autoStartDispatch(
            DispatchRequestDto.AutoStart requestDto) {

        Route route = routeRepository.findById(requestDto.getRouteId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 노선 ID: "
                                        + requestDto.getRouteId()
                        )
                );

        Driver driver = driverRepository
                .findFirstByWorkStatusOrderByIdAsc("STANDBY")
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "현재 배차 가능한 대기 기사가 없습니다."
                        )
                );

        Vehicle vehicle = vehicleRepository
                .findFirstByStatusOrderByIdAsc("INACTIVE")
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "현재 배차 가능한 운행 대기 차량이 없습니다."
                        )
                );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime plannedEndTime = now.plusHours(1);

        Dispatch dispatch = Dispatch.builder()
                .route(route)
                .vehicle(vehicle)
                .driver(driver)
                .plannedStartTime(now)
                .plannedEndTime(plannedEndTime)
                .dispatchStatus("SCHEDULED")
                .build();

        Dispatch savedDispatch = dispatchRepository.save(dispatch);

        return updateStatus(
                savedDispatch.getId(),
                createInProgressRequest()
        );
    }

    private DispatchRequestDto.UpdateStatus createInProgressRequest() {
        return new DispatchRequestDto.UpdateStatus("IN_PROGRESS");
    }

    public List<DispatchResponseDto> getAllDispatches() {
        return dispatchRepository.findAll()
                .stream()
                .map(DispatchResponseDto::new)
                .collect(Collectors.toList());
    }

    public DispatchResponseDto getDispatchById(Long id) {
        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 배차 정보가 존재하지 않습니다. ID: "
                                        + id
                        )
                );

        return new DispatchResponseDto(dispatch);
    }

    @Transactional
    public DispatchResponseDto updateStatus(
            Long id,
            DispatchRequestDto.UpdateStatus requestDto) {

        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 배차 정보가 존재하지 않습니다. ID: "
                                        + id
                        )
                );

        String newStatus = requestDto.getDispatchStatus();

        if (!"IN_PROGRESS".equals(newStatus)
                && !"COMPLETED".equals(newStatus)
                && !"CANCELED".equals(newStatus)) {
            throw new IllegalArgumentException(
                    "올바르지 않은 배차 상태입니다."
            );
        }

        String currentStatus = dispatch.getDispatchStatus();

        if ("SCHEDULED".equals(currentStatus)) {

            if (!"IN_PROGRESS".equals(newStatus)
                    && !"CANCELED".equals(newStatus)) {
                throw new IllegalArgumentException(
                        "배차 대기 상태에서는 운행 시작 또는 취소만 가능합니다."
                );
            }

        } else if ("IN_PROGRESS".equals(currentStatus)) {

            if (!"COMPLETED".equals(newStatus)) {
                throw new IllegalArgumentException(
                        "운행 중 상태에서는 운행 종료만 가능합니다."
                );
            }

        } else {
            throw new IllegalArgumentException(
                    "이미 종료되거나 취소된 배차는 상태를 변경할 수 없습니다."
            );
        }

        Driver driver = dispatch.getDriver();
        Vehicle vehicle = dispatch.getVehicle();

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime actualStartTime =
                dispatch.getActualStartTime();

        LocalDateTime actualEndTime =
                dispatch.getActualEndTime();

        if ("IN_PROGRESS".equals(newStatus)) {

            if ("RETIRED".equals(driver.getWorkStatus())) {
                throw new IllegalArgumentException(
                        "퇴직한 기사는 운행을 시작할 수 없습니다."
                );
            }

            if ("MAINTENANCE".equals(vehicle.getStatus())) {
                throw new IllegalArgumentException(
                        "정비 중인 차량은 운행을 시작할 수 없습니다."
                );
            }

            actualStartTime = now;

            driver.changeWorkStatus("ON_DUTY");
            vehicle.updateStatus("ACTIVE");

        } else if ("COMPLETED".equals(newStatus)) {

            actualEndTime = now;

            driver.changeWorkStatus("STANDBY");

        } else if ("CANCELED".equals(newStatus)) {

            driver.changeWorkStatus("OFF_DUTY");
        }

        Dispatch updatedDispatch = Dispatch.builder()
                .id(dispatch.getId())
                .route(dispatch.getRoute())
                .vehicle(dispatch.getVehicle())
                .driver(dispatch.getDriver())
                .plannedStartTime(dispatch.getPlannedStartTime())
                .plannedEndTime(dispatch.getPlannedEndTime())
                .actualStartTime(actualStartTime)
                .actualEndTime(actualEndTime)
                .dispatchStatus(newStatus)
                .createdAt(dispatch.getCreatedAt())
                .updatedAt(now)
                .build();

        return new DispatchResponseDto(
                dispatchRepository.save(updatedDispatch)
        );
    }

    @Transactional
    public void deleteDispatch(Long id) {

        Dispatch dispatch = dispatchRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 배차 정보가 존재하지 않습니다. ID: "
                                        + id
                        )
                );

        if ("IN_PROGRESS".equals(dispatch.getDispatchStatus())) {
            throw new IllegalArgumentException(
                    "운행 중인 배차는 삭제할 수 없습니다."
            );
        }

        dispatchRepository.delete(dispatch);
    }
}