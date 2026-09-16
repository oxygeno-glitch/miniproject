package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DriverRequestDto {
    private String name;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseType;
}