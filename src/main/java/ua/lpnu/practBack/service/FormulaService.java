package ua.lpnu.practBack.service;

import lombok.RequiredArgsConstructor;
import org.matheclipse.core.eval.ExprEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.lpnu.practBack.dto.FormulaRequest;
import ua.lpnu.practBack.dto.FormulaResponse;
import ua.lpnu.practBack.entity.Formula;
import ua.lpnu.practBack.repository.FormulaRepository;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormulaService {

    private final FormulaRepository formulaRepository;

    @Transactional(readOnly = true)
    public List<FormulaResponse> getAll() {
        return formulaRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormulaResponse getById(Long id) {
        var formula = formulaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Формулу не знайдено"));
        return mapToDTO(formula);
    }

    @Transactional(readOnly = true)
    public List<FormulaResponse> searchByName(String name) {
        return formulaRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public FormulaResponse create(FormulaRequest dto) {
        validateSyntax(dto.getScripts());
        validateVariables(dto.getScripts(), dto.getParameters());
        var formula = Formula.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .scripts(dto.getScripts())
                .parameters(dto.getParameters())
                .build();

        return mapToDTO(formulaRepository.save(formula));
    }

    @Transactional
    public FormulaResponse update(Long id, FormulaRequest dto) {
        var formula = formulaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Формулу не знайдено"));

        if (dto.getScripts() != null) {
            validateSyntax(dto.getScripts());
        }

        // Оновлюємо лише ті поля, які прийшли в запиті (не null)
        if (dto.getName() != null) formula.setName(dto.getName());
        if (dto.getDescription() != null) formula.setDescription(dto.getDescription());
        if (dto.getScripts() != null) formula.setScripts(dto.getScripts());
        if (dto.getParameters() != null) formula.setParameters(dto.getParameters());

        validateVariables(formula.getScripts(), formula.getParameters());

        return mapToDTO(formulaRepository.save(formula));
    }

    @Transactional
    public void delete(Long id) {
        formulaRepository.deleteById(id);
    }

    private FormulaResponse mapToDTO(Formula f) {
        return FormulaResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .description(f.getDescription())
                .scripts(f.getScripts())
                .parameters(f.getParameters())
                .build();
    }

    private void validateSyntax(List<Formula.FormulaScript> scripts) {
        if (scripts == null) return;

        ExprEvaluator evaluator = new ExprEvaluator();
        for (Formula.FormulaScript script : scripts) {
            try {
                // Пробуємо просто розпарсити вираз
                evaluator.parse(script.getExpression());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Синтаксична помилка у формулі для '" + script.getTarget() + "': " + script.getExpression()
                );
            }
        }
    }

    private void validateVariables(List<Formula.FormulaScript> scripts, List<Formula.FormulaParam> parameters) {
        if (scripts == null || parameters == null) return;

        // 1. Збираємо всі задекларовані параметри (і вхідні, і вихідні)
        Set<String> declaredVars = parameters.stream()
                .map(Formula.FormulaParam::getVar)
                .collect(Collectors.toSet());

        Set<String> symjaBuiltIns = Set.of("Sin", "Cos", "Tan", "Log", "Sqrt", "E", "Pi");
        Pattern pattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

        for (Formula.FormulaScript script : scripts) {

            // 2. Нова перевірка: чи описана цільова змінна (щоб клієнт знав її одиниці виміру)
            if (!declaredVars.contains(script.getTarget())) {
                throw new IllegalArgumentException(
                        "Цільова змінна '" + script.getTarget() + "' не знайдена в parameters. " +
                                "Усі результати повинні мати опис та одиниці виміру."
                );
            }

            // 3. Перевірка змінних всередині самої формули
            Matcher matcher = pattern.matcher(script.getExpression());
            while (matcher.find()) {
                String varName = matcher.group();

                if (!declaredVars.contains(varName) && !symjaBuiltIns.contains(varName)) {
                    throw new IllegalArgumentException(
                            "Невідома змінна '" + varName + "' у формулі: '" + script.getExpression() +
                                    "'. Вона повинна бути задекларована в parameters."
                    );
                }
            }
        }
    }
}