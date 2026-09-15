package ua.lpnu.practBack.dto;

import lombok.Builder;
import lombok.Data;
import ua.lpnu.practBack.entity.Formula.FormulaParam;
import ua.lpnu.practBack.entity.Formula.FormulaScript;

import java.util.List;

@Data
@Builder
public class FormulaResponse {
    private Long id;
    private String name;
    private String description;
    private List<FormulaScript> scripts;
    private List<FormulaParam> parameters;
}