package ua.lpnu.practBack.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CalculationRequest {
    private Long formulaId;
    private String name;
    private Map<String, Double> inputs;
    private Map<String, String> inputUnits;
}