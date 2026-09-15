package ua.lpnu.practBack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.lpnu.practBack.dto.CalculationRequest;
import ua.lpnu.practBack.dto.CalculationResponse;
import ua.lpnu.practBack.service.CalculationService;

import java.util.List;

@RestController
@RequestMapping("/api/calculations")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculationService calculationService;

    @GetMapping("/{id}")
    public ResponseEntity<CalculationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(calculationService.getById(id));
    }

    @GetMapping("/formula/{formulaId}")
    public ResponseEntity<List<CalculationResponse>> getAllByFormulaId(@PathVariable Long formulaId) {
        return ResponseEntity.ok(calculationService.getAllByFormulaId(formulaId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CalculationResponse>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(calculationService.searchByName(name));
    }

    @GetMapping("/formula/{formulaId}/search")
    public ResponseEntity<List<CalculationResponse>> searchByFormulaAndName(
            @PathVariable Long formulaId,
            @RequestParam String name) {
        return ResponseEntity.ok(calculationService.searchByFormulaAndName(formulaId, name));
    }

    @PostMapping
    public ResponseEntity<CalculationResponse> createCalculation(@RequestBody CalculationRequest request) {
        CalculationResponse created = calculationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CalculationResponse> updateAndCalculate(
            @PathVariable Long id,
            @RequestBody CalculationRequest request) {
        return ResponseEntity.ok(calculationService.updateAndCalculate(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCalculation(@PathVariable Long id) {
        calculationService.deleteCalculation(id);
        return ResponseEntity.noContent().build();
    }
}