package ua.lpnu.practBack.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class StandardizationService {

    private static final double[] E24 = {
            1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0,
            3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5, 8.2, 9.1, 10
    };

    public Double toNearestE24(Double value) {
        if (value == null || value <= 0) return value;

        double exponent = Math.floor(Math.log10(value));
        double multiplier = Math.pow(10, exponent);

        double normalized = value / multiplier;

        double nearest = findNearestInSeries(normalized);

        // Округлюємо можливі похибки плаваючої крапки (наприклад 91.00000000001)
        double result = nearest * multiplier;
        return Math.round(result * 1000000.0) / 1000000.0;
    }

    private double findNearestInSeries(double target) {
        double minDiff = Double.MAX_VALUE;
        double nearest = E24[0];

        for (double val : E24) {
            double diff = Math.abs(target - val);
            if (diff < minDiff) {
                minDiff = diff;
                nearest = val;
            }
        }
        return nearest;
    }

    public Map<String, Double> standardizeResults(Map<String, Double> results) {
        Map<String, Double> standardized = new TreeMap<>();
        if (results == null) return standardized;

        results.forEach((key, val) -> {
            // Фільтруємо по ключах R (резистори), C (конденсатори), L (індуктивності)
            if (key.matches("^[RCL].*")) {
                standardized.put(key, toNearestE24(val));
            } else {
                standardized.put(key, val);
            }
        });

        return standardized;
    }

    // НОВИЙ МЕТОД: Розрахунок відсоткової похибки
    public Map<String, Double> calculateDeviations(Map<String, Double> exactResults, Map<String, Double> stdResults) {
        Map<String, Double> deviations = new HashMap<>();
        if (exactResults == null || stdResults == null) return deviations;

        stdResults.forEach((key, stdVal) -> {
            Double exactVal = exactResults.get(key);

            // Якщо значення змінилося в процесі стандартизації
            if (exactVal != null && exactVal != 0 && !exactVal.equals(stdVal)) {
                // Формула: ((Нове Значення - Старе Значення) / Старе Значення) * 100%
                double deviationPercent = ((stdVal - exactVal) / exactVal) * 100.0;

                // Округлюємо до 2 знаків після коми (наприклад, 3.41 %)
                deviationPercent = Math.round(deviationPercent * 100.0) / 100.0;

                deviations.put(key, deviationPercent);
            }
        });

        return deviations;
    }
}