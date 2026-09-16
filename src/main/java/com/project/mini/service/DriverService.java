package com.project.mini.service;

import com.project.mini.dto.DriverRequestDto;
import com.project.mini.dto.DriverResponseDto;
import com.project.mini.entity.Driver;
import com.project.mini.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;

    @Transactional
    public DriverResponseDto registerDriver(DriverRequestDto requestDto) {
        driverRepository.findByPhoneNumber(requestDto.getPhoneNumber())
                .ifPresent(d -> {
                    throw new IllegalArgumentException(
                            "이미 등록된 전화번호입니다: "
                                    + requestDto.getPhoneNumber()
                    );
                });

        driverRepository.findByLicenseNumber(requestDto.getLicenseNumber())
                .ifPresent(d -> {
                    throw new IllegalArgumentException(
                            "이미 등록된 면허번호입니다: "
                                    + requestDto.getLicenseNumber()
                    );
                });

        Driver driver = Driver.builder()
                .name(requestDto.getName())
                .phoneNumber(requestDto.getPhoneNumber())
                .licenseNumber(requestDto.getLicenseNumber())
                .licenseType(requestDto.getLicenseType())
                .workStatus("OFF_DUTY")
                .build();

        Driver savedDriver = driverRepository.save(driver);

        return new DriverResponseDto(savedDriver);
    }

    public List<DriverResponseDto> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(DriverResponseDto::new)
                .collect(Collectors.toList());
    }

    public DriverResponseDto getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 운전자가 존재하지 않습니다. ID: " + id
                        )
                );

        return new DriverResponseDto(driver);
    }

    @Transactional
    public DriverResponseDto updateWorkStatus(
            Long id,
            String workStatus
    ) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 운전자가 존재하지 않습니다. ID: " + id
                        )
                );

        if (!"ON_DUTY".equals(workStatus)
                && !"STANDBY".equals(workStatus)
                && !"OFF_DUTY".equals(workStatus)
                && !"RETIRED".equals(workStatus)) {

            throw new IllegalArgumentException(
                    "올바르지 않은 기사 상태입니다."
            );
        }

        driver.changeWorkStatus(workStatus);

        return new DriverResponseDto(driver);
    }

    @Transactional
    public void deleteDriver(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "해당 운전자가 존재하지 않습니다. ID: " + id
            );
        }

        driverRepository.deleteById(id);
    }
}