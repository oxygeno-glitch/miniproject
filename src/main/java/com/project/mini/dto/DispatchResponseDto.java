package com.project.mini.dto;

import com.project.mini.entity.Dispatch;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DispatchResponseDto {

    private Long id;

    private Long routeId;
    private String routeNumber;
    private String routeName;

    private Long vehicleId;
    private String vehiclePlateNumber;

    private Long driverId;
    private String driverName;

    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    private String dispatchStatus;
    private LocalDateTime createdAt;

    public DispatchResponseDto(Dispatch dispatch) {
        this.id = dispatch.getId();

        this.routeId = dispatch.getRoute().getId();
        this.routeNumber = dispatch.getRoute().getRouteNumber();
        this.routeName = dispatch.getRoute().getRouteName();

        this.vehicleId = dispatch.getVehicle().getId();
        this.vehiclePlateNumber = dispatch.getVehicle().getPlateNumber();

        this.driverId = dispatch.getDriver().getId();
        this.driverName = dispatch.getDriver().getName();

        this.plannedStartTime = dispatch.getPlannedStartTime();
        this.plannedEndTime = dispatch.getPlannedEndTime();
        this.actualStartTime = dispatch.getActualStartTime();
        this.actualEndTime = dispatch.getActualEndTime();

        this.dispatchStatus = dispatch.getDispatchStatus();
        this.createdAt = dispatch.getCreatedAt();
    }
}