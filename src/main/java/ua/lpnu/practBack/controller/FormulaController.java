package ua.lpnu.practBack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.lpnu.practBack.dto.FormulaRequest;
import ua.lpnu.practBack.dto.FormulaResponse;
import ua.lpnu.practBack.service.FormulaService;

import java.util.List;

@RestController
@RequestMapping("/api/formulas")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaService formulaService;

    @GetMapping
    public ResponseEntity<List<FormulaResponse>> getAllFormulas() {
        return ResponseEntity.ok(formulaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormulaResponse> getFormulaById(@PathVariable Long id) {
        return ResponseEntity.ok(formulaService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FormulaResponse>> searchFormulas(@RequestParam String name) {
        return ResponseEntity.ok(formulaService.searchByName(name));
    }

    @PostMapping
    public ResponseEntity<FormulaResponse> createFormula(@RequestBody FormulaRequest request) {
        FormulaResponse created = formulaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormulaResponse> updateFormula(
            @PathVariable Long id,
            @RequestBody FormulaRequest request) {
        return ResponseEntity.ok(formulaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFormula(@PathVariable Long id) {
        formulaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}