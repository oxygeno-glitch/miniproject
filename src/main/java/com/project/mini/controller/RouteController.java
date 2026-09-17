package com.project.mini.controller;

import com.project.mini.dto.RouteRequestDto;
import com.project.mini.dto.RouteResponseDto;
import com.project.mini.dto.StopRequestDto;
import com.project.mini.dto.StopResponseDto;
import com.project.mini.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/stops")
    public ResponseEntity<StopResponseDto> createStop(@RequestBody StopRequestDto.Create requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createStop(requestDto));
    }

    @GetMapping("/stops")
    public ResponseEntity<List<StopResponseDto>> getAllStops() {
        return ResponseEntity.ok(routeService.getAllStops());
    }

    @PostMapping("/routes")
    public ResponseEntity<RouteResponseDto> createRoute(@RequestBody RouteRequestDto.Create requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(requestDto));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponseDto>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<RouteResponseDto> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @PatchMapping("/routes/{id}/status")
    public ResponseEntity<RouteResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody RouteRequestDto.UpdateStatus requestDto) {

        return ResponseEntity.ok(
                routeService.updateStatus(id, requestDto.getStatus())
        );
    }
}