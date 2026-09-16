package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class RouteRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        private String routeNumber;
        private String routeName;
        private String routeType;
        private List<RouteStopOrderDto> stops;
    }

    @Getter
    @NoArgsConstructor
    public static class RouteStopOrderDto {
        private Long stopId;
        private Integer stopSequence;
        private Integer estimatedMinutes;
    }
}