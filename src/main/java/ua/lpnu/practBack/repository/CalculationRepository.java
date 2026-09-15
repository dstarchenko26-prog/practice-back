package ua.lpnu.practBack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.lpnu.practBack.entity.Calculation;

import java.util.List;

@Repository
public interface CalculationRepository extends JpaRepository<Calculation, Long> {

    List<Calculation> findAllByFormulaId(Long formulaId);
    List<Calculation> findByNameContainingIgnoreCase(String name);
    List<Calculation> findAllByFormulaIdAndNameContainingIgnoreCase(Long formulaId, String name);

}