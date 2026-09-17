package com.project.mini.controller;

import com.project.mini.dto.DispatchRequestDto;
import com.project.mini.dto.DispatchResponseDto;
import com.project.mini.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatches")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping
    public ResponseEntity<DispatchResponseDto> createDispatch(
            @RequestBody DispatchRequestDto.Create requestDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dispatchService.createDispatch(requestDto));
    }

    @GetMapping
    public ResponseEntity<List<DispatchResponseDto>> getAllDispatches() {
        return ResponseEntity.ok(dispatchService.getAllDispatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DispatchResponseDto> getDispatchById(
            @PathVariable Long id) {

        return ResponseEntity.ok(dispatchService.getDispatchById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DispatchResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody DispatchRequestDto.UpdateStatus requestDto) {

        return ResponseEntity.ok(
                dispatchService.updateStatus(id, requestDto)
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteDispatches(
            @RequestBody List<Long> ids) {

        dispatchService.deleteDispatches(ids);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDispatch(
            @PathVariable Long id) {

        dispatchService.deleteDispatch(id);

        return ResponseEntity.noContent().build();
    }
}