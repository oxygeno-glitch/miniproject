package com.project.mini.service;

import com.project.mini.dto.RouteRequestDto;
import com.project.mini.dto.RouteResponseDto;
import com.project.mini.dto.StopRequestDto;
import com.project.mini.dto.StopResponseDto;
import com.project.mini.entity.Route;
import com.project.mini.entity.RouteStop;
import com.project.mini.entity.Stop;
import com.project.mini.repository.RouteRepository;
import com.project.mini.repository.RouteStopRepository;
import com.project.mini.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteStopRepository routeStopRepository;

    /**
     * 정류장 신규 등록
     */
    @Transactional
    public StopResponseDto createStop(StopRequestDto.Create requestDto) {
        Stop stop = Stop.builder()
                .stopCode(requestDto.getStopCode())
                .stopName(requestDto.getStopName())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .build();

        return new StopResponseDto(stopRepository.save(stop));
    }

    /**
     * 전체 정류장 목록 조회
     */
    public List<StopResponseDto> getAllStops() {
        return stopRepository.findAll().stream()
                .map(StopResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 노선 신규 등록 (정류장 매핑 포함)
     */
    @Transactional
    public RouteResponseDto createRoute(RouteRequestDto.Create requestDto) {
        Route route = Route.builder()
                .routeNumber(requestDto.getRouteNumber())
                .routeName(requestDto.getRouteName())
                .routeType(requestDto.getRouteType())
                .status("ACTIVE")
                .build();

        Route savedRoute = routeRepository.save(route);
        List<RouteStop> savedRouteStops = new ArrayList<>();

        if (requestDto.getStops() != null && !requestDto.getStops().isEmpty()) {
            for (RouteRequestDto.RouteStopOrderDto stopOrder : requestDto.getStops()) {
                Stop stop = stopRepository.findById(stopOrder.getStopId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 정류장 ID: " + stopOrder.getStopId()));

                RouteStop routeStop = RouteStop.builder()
                        .route(savedRoute)
                        .stop(stop)
                        .stopSequence(stopOrder.getStopSequence())
                        .estimatedMinutes(stopOrder.getEstimatedMinutes() != null ? stopOrder.getEstimatedMinutes() : 0)
                        .build();

                savedRouteStops.add(routeStopRepository.save(routeStop));
            }
        }

        return new RouteResponseDto(savedRoute, savedRouteStops);
    }

    /**
     * 전체 노선 목록 조회
     */
    public List<RouteResponseDto> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(RouteResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 특정 노선 단건 조회
     */
    public RouteResponseDto getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 노선이 존재하지 않습니다. ID: " + id
                        )
                );

        List<RouteStop> routeStops =
                routeStopRepository.findByRouteIdOrderByStopSequenceAsc(id);

        return new RouteResponseDto(route, routeStops);
    }
}