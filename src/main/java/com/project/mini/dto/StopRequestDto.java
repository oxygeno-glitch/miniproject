package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class StopRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        private String stopCode;
        private String stopName;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}