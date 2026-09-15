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
    // private final StandardizationService standardizationService;

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
            calculation.setInputUnits(request.getInputUnits() != null ? request.getInputUnits() : new HashMap<>());

            var formula = calculation.getFormula();

            // Нормалізуємо та рахуємо
            Map<String, Double> normalizedInputs = normalizeInputs(formula, request.getInputs(), request.getInputUnits());
            Map<String, Double> allCalculatedVars = mathService.runAutoSolver(formula, normalizedInputs);

            // Відділяємо результати від вхідних даних
            Map<String, Double> results = new HashMap<>(allCalculatedVars);
            request.getInputs().keySet().forEach(results::remove);

            // Стандартизація (ПОКИ ЩО ЗАГЛУШКА)
            Map<String, Double> stdResults = new HashMap<>(); // standardizationService.standardizeResults(results);
            Map<String, Double> deviations = new HashMap<>(); // standardizationService.calculateDeviations(results, stdResults);

            calculation.setResults(results);
            calculation.setStandardizedResults(stdResults);
            calculation.setDeviations(deviations);
        }

        return mapToDto(calculationRepository.save(calculation));
    }

    @Transactional
    public void deleteCalculation(Long id) {
        calculationRepository.deleteById(id);
    }

    private Map<String, Double> normalizeInputs(Formula formula, Map<String, Double> inputs, Map<String, String> inputUnits) {
        Map<String, Double> normalized = new HashMap<>();
        if (inputs == null) return normalized;

        inputs.forEach((key, val) -> {
            double multiplier = 1.0;

            var paramOpt = formula.getParameters().stream()
                    .filter(p -> p.getVar().equals(key))
                    .findFirst();

            if (paramOpt.isPresent() && inputUnits != null && inputUnits.containsKey(key)) {
                String unitName = inputUnits.get(key);
                if (paramOpt.get().getUnits() != null) {
                    multiplier = paramOpt.get().getUnits().stream()
                            .filter(u -> u.getName().equals(unitName))
                            .findFirst()
                            .map(Formula.UnitDefinition::getMult)
                            .orElse(1.0);
                }
            }
            normalized.put(key, val * multiplier);
        });
        return normalized;
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