package ua.lpnu.practBack.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.lpnu.practBack.dto.CalculationRequest;
import ua.lpnu.practBack.dto.CalculationResponse;
import ua.lpnu.practBack.entity.Calculation;
import ua.lpnu.practBack.entity.Formula;
import ua.lpnu.practBack.repository.CalculationRepository;
import ua.lpnu.practBack.repository.FormulaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final FormulaRepository formulaRepository;
    private final CalculationRepository calculationRepository;
    private final MathService mathService;
    private final StandardizationService standardizationService;

    @Transactional(readOnly = true)
    public CalculationResponse getById(Long id) {
        var calc = calculationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Розрахунок не знайдено"));
        return mapToDto(calc);
    }

    @Transactional(readOnly = true)
    public List<CalculationResponse> getAllByFormulaId(Long formulaId) {
        return calculationRepository.findAllByFormulaId(formulaId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalculationResponse> searchByName(String name) {
        return calculationRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalculationResponse> searchByFormulaAndName(Long formulaId, String name) {
        return calculationRepository.findAllByFormulaIdAndNameContainingIgnoreCase(formulaId, name).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CalculationResponse create(CalculationRequest request) {
        var formula = formulaRepository.findById(request.getFormulaId())
                .orElseThrow(() -> new RuntimeException("Формулу не знайдено"));

        String calcName = (request.getName() != null && !request.getName().isBlank())
                ? request.getName()
                : formula.getName() + " - Новий розрахунок";

        var calculation = Calculation.builder()
                .formula(formula)
                .name(calcName)
                .inputs(new HashMap<>())
                .inputUnits(new HashMap<>())
                .results(new HashMap<>())
                .standardizedResults(new HashMap<>())
                .deviations(new HashMap<>())
                .build();

        return mapToDto(calculationRepository.save(calculation));
    }

    @Transactional
    public CalculationResponse updateAndCalculate(Long id, CalculationRequest request) {
        var calculation = calculationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Розрахунок не знайдено"));

        if (request.getName() != null && !request.getName().isBlank()) {
            calculation.setName(request.getName());
        }

        if (request.getInputs() != null) {
            calculation.setInputs(request.getInputs());

            Map<String, String> inputUnits = request.getInputUnits() != null ? request.getInputUnits() : new HashMap<>();
            calculation.setInputUnits(inputUnits);

            var formula = calculation.getFormula();

            Map<String, Double> normalizedInputs = new HashMap<>();
            request.getInputs().forEach((key, val) -> {
                double mult = getUnitMultiplier(formula, key, inputUnits);
                normalizedInputs.put(key, val * mult);
            });

            Map<String, Double> idealVars = mathService.runAutoSolver(formula, normalizedInputs);
            Map<String, Double> exactResults = new HashMap<>(idealVars);
            normalizedInputs.keySet().forEach(exactResults::remove); // Видаляємо введені користувачем дані

            Map<String, Double> e24Overrides = standardizationService.getE24Overrides(exactResults);

            Map<String, Double> stdResultsBase = new HashMap<>();
            if (!e24Overrides.isEmpty()) {
                Map<String, Double> realWorldInputs = new HashMap<>(normalizedInputs);
                realWorldInputs.putAll(e24Overrides);

                Map<String, Double> realWorldVars = mathService.runAutoSolver(formula, realWorldInputs);

                stdResultsBase.putAll(realWorldVars);
                normalizedInputs.keySet().forEach(stdResultsBase::remove);
                stdResultsBase.putAll(e24Overrides);
            } else {
                stdResultsBase.putAll(exactResults);
            }

            Map<String, Double> finalExactResults = new HashMap<>();
            exactResults.forEach((key, val) -> {
                double mult = getUnitMultiplier(formula, key, inputUnits);
                finalExactResults.put(key, val / mult);
            });

            Map<String, Double> finalStdResults = new HashMap<>();
            stdResultsBase.forEach((key, val) -> {
                double mult = getUnitMultiplier(formula, key, inputUnits);
                finalStdResults.put(key, val / mult);
            });

            calculation.setResults(finalExactResults);
            calculation.setStandardizedResults(finalStdResults);

            calculation.setDeviations(standardizationService.calculateDeviations(finalExactResults, finalStdResults));
        }

        return mapToDto(calculationRepository.save(calculation));
    }

    @Transactional
    public void deleteCalculation(Long id) {
        calculationRepository.deleteById(id);
    }

    private double getUnitMultiplier(Formula formula, String key, Map<String, String> inputUnits) {
        if (inputUnits == null || !inputUnits.containsKey(key)) return 1.0;
        String unitName = inputUnits.get(key);

        var paramOpt = formula.getParameters().stream()
                .filter(p -> p.getVar().equals(key))
                .findFirst();

        if (paramOpt.isPresent() && paramOpt.get().getUnits() != null) {
            return paramOpt.get().getUnits().stream()
                    .filter(u -> u.getName().equals(unitName))
                    .findFirst()
                    .map(Formula.UnitDefinition::getMult)
                    .orElse(1.0);
        }
        return 1.0;
    }

    private CalculationResponse mapToDto(Calculation calc) {
        return CalculationResponse.builder()
                .id(calc.getId())
                .name(calc.getName())
                .formulaId(calc.getFormula().getId())
                .formulaName(calc.getFormula().getName())
                .inputs(calc.getInputs())
                .inputUnits(calc.getInputUnits())
                .results(calc.getResults())
                .standardizedResults(calc.getStandardizedResults())
                .deviations(calc.getDeviations())
                .createdAt(calc.getCreatedAt())
                .build();
    }
}