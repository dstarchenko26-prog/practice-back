package ua.lpnu.practBack.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.lpnu.practBack.dto.FormulaRequest;
import ua.lpnu.practBack.dto.FormulaResponse;
import ua.lpnu.practBack.entity.Formula;
import ua.lpnu.practBack.repository.FormulaRepository;

import java.util.List;
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

        // Оновлюємо лише ті поля, які прийшли в запиті (не null)
        if (dto.getName() != null) formula.setName(dto.getName());
        if (dto.getDescription() != null) formula.setDescription(dto.getDescription());
        if (dto.getScripts() != null) formula.setScripts(dto.getScripts());
        if (dto.getParameters() != null) formula.setParameters(dto.getParameters());

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
}