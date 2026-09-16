package com.project.mini.repository;

import com.project.mini.entity.VehicleMaintenance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {

    @Override
    @EntityGraph(attributePaths = {"vehicle"})
    List<VehicleMaintenance> findAll();
}