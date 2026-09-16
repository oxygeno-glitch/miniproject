package com.project.mini.repository;

import com.project.mini.entity.Dispatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    // N+1 방지를 위해 route, vehicle, driver를 한번에 Join해서 가져옵니다.
    @Override
    @EntityGraph(attributePaths = {"route", "vehicle", "driver"})
    List<Dispatch> findAll();

    @Override
    @EntityGraph(attributePaths = {"route", "vehicle", "driver"})
    Optional<Dispatch> findById(Long id);
}