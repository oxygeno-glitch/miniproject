package com.project.mini.dto;

import com.project.mini.entity.Route;
import com.project.mini.entity.RouteStop;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class RouteResponseDto {
    private Long id;
    private String routeNumber;
    private String routeName;
    private String routeType;
    private String status;
    private List<RouteStopResponseDto> routeStops;
    private LocalDateTime createdAt;

    public RouteResponseDto(Route route) {
        this.id = route.getId();
        this.routeNumber = route.getRouteNumber();
        this.routeName = route.getRouteName();
        this.routeType = route.getRouteType();
        this.status = route.getStatus();
        this.createdAt = route.getCreatedAt();
    }

    public RouteResponseDto(Route route, List<RouteStop> routeStops) {
        this(route);
        if (routeStops != null) {
            this.routeStops = routeStops.stream()
                    .map(RouteStopResponseDto::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    public static class RouteStopResponseDto {
        private Long routeStopId;
        private Long stopId;
        private String stopCode;
        private String stopName;
        private Integer stopSequence;
        private Integer estimatedMinutes;

        public RouteStopResponseDto(RouteStop routeStop) {
            this.routeStopId = routeStop.getId();
            this.stopId = routeStop.getStop().getId();
            this.stopCode = routeStop.getStop().getStopCode();
            this.stopName = routeStop.getStop().getStopName();
            this.stopSequence = routeStop.getStopSequence();
            this.estimatedMinutes = routeStop.getEstimatedMinutes();
        }
    }
}