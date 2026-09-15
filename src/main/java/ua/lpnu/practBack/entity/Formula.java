package ua.lpnu.practBack.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "formulas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<FormulaScript> scripts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<FormulaParam> parameters;

    // Внутрішні класи для правильного мапінгу JSON-структур
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaScript {
        private String target;     // Наприклад: "R"
        private String expression; // Наприклад: "U/I"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaParam {
        private String var;        // Наприклад: "U"
        private String label;      // Наприклад: "Напруга (В)"
        private List<UnitDefinition> units;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitDefinition {
        private String name;       // Наприклад: "мВ"
        private Double mult;       // Наприклад: 0.001
    }
}