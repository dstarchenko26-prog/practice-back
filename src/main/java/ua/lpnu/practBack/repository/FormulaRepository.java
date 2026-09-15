package ua.lpnu.practBack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.lpnu.practBack.entity.Formula;

import java.util.List;

@Repository
public interface FormulaRepository extends JpaRepository<Formula, Long> {

    List<Formula> findByNameContainingIgnoreCase(String name);

}