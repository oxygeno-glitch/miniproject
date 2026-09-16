package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DispatchRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        private Long routeId;
        private Long vehicleId;
        private Long driverId;
        private LocalDateTime plannedStartTime;
        private LocalDateTime plannedEndTime;
    }

    @Getter
    @NoArgsConstructor
    public static class AutoStart {
        private Long routeId;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateStatus {
        private String dispatchStatus;

        public UpdateStatus(String dispatchStatus) {
            this.dispatchStatus = dispatchStatus;
        }
    }
}