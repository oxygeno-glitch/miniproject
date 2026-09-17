package com.project.mini.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DriverRequestDto {

    private String name;
    private String gender;
    private Integer age;
    private String address;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseType;
}