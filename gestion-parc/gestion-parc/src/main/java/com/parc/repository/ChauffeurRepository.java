package com.parc.repository;

import com.parc.domain.entity.Chauffeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChauffeurRepository extends JpaRepository<Chauffeur, Long> {

    Optional<Chauffeur> findByNumeroPermis(String numeroPermis);
    boolean existsByNumeroPermis(String numeroPermis);
    boolean existsByParcId(Long parcId);
    Optional<Chauffeur> findByEmail(String email);

    // ✅ AJOUT : charger le chauffeur avec son parc ET le chef du parc en une seule requête
    // Cela évite le problème de lazy loading (NullPointerException sur getParc().getChef())
    @Query("SELECT c FROM Chauffeur c " +
           "LEFT JOIN FETCH c.parc p " +
           "LEFT JOIN FETCH p.chef " +
           "WHERE c.id = :id")
    Optional<Chauffeur> findByIdWithParcAndChef(@Param("id") Long id);
}