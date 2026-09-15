package ua.lpnu.practBack.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class CalculationResponse {
    private Long id;
    private String name;

    private Long formulaId;
    private String formulaName;

    private Map<String, Double> inputs;
    private Map<String, String> inputUnits;
    private Map<String, Double> results;

    private Map<String, Double> standardizedResults;
    private Map<String, Double> deviations;

    private Instant createdAt;
}