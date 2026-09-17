package com.project.mini.dto;

import com.project.mini.entity.Driver;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DriverResponseDto {
    private Long id;
    private String name;
    private String gender;
    private Integer age;
    private String address;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseType;
    private String workStatus;
    private LocalDateTime createdAt;

    public DriverResponseDto(Driver driver) {
        this.id = driver.getId();
        this.name = driver.getName();
        this.gender = driver.getGender();
        this.age = driver.getAge();
        this.address = driver.getAddress();
        this.phoneNumber = driver.getPhoneNumber();
        this.licenseNumber = driver.getLicenseNumber();
        this.licenseType = driver.getLicenseType();
        this.workStatus = driver.getWorkStatus();
        this.createdAt = driver.getCreatedAt();
    }
}