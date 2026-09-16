package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VehicleRequestDto {
    private String plateNumber;
    private String modelName;
    private String manufacturer;
    private Integer modelYear;
}