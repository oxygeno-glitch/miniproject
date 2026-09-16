package com.project.mini.dto;

import com.project.mini.entity.Stop;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class StopResponseDto {
    private Long id;
    private String stopCode;
    private String stopName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;

    public StopResponseDto(Stop stop) {
        this.id = stop.getId();
        this.stopCode = stop.getStopCode();
        this.stopName = stop.getStopName();
        this.latitude = stop.getLatitude();
        this.longitude = stop.getLongitude();
        this.createdAt = stop.getCreatedAt();
    }
}