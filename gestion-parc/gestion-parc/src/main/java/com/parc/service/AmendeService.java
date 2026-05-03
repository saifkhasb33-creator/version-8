package com.parc.service;

import com.parc.dto.AmendeDTO;
import com.parc.domain.entity.Amende;
import com.parc.domain.entity.Chauffeur;
import com.parc.domain.entity.Vehicule;
import com.parc.repository.AmendeRepository;
import com.parc.repository.ChauffeurRepository;
import com.parc.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmendeService {

    private final AmendeRepository amendeRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final NotificationService notificationService;

    // === Conversion DTO <=> Entité ===
    private AmendeDTO toDTO(Amende amende) {
        AmendeDTO dto = new AmendeDTO();
        dto.setId(amende.getId());
        dto.setMontant(amende.getMontant());
        dto.setDateInfraction(amende.getDateInfraction());
        dto.setLieuInfraction(amende.getLieuInfraction());
        dto.setMotif(amende.getMotif());
        dto.setDescription(amende.getDescription());
        dto.setPhoto(amende.getPhoto());
        dto.setStatut(amende.getStatut());
        dto.setDateDeclaration(amende.getDateDeclaration());
        dto.setPayee(amende.getPayee());
        if (amende.getChauffeur() != null) {
            dto.setChauffeurId(amende.getChauffeur().getId());
            dto.setChauffeurNom(amende.getChauffeur().getNom() + " " + amende.getChauffeur().getPrenom());
        }
        if (amende.getVehicule() != null) {
            dto.setVehiculeId(amende.getVehicule().getId());
            dto.setVehiculeMatricule(amende.getVehicule().getMatricule());
        }
        return dto;
    }

    private Amende toEntity(AmendeDTO dto) {
        Amende amende = new Amende();
        amende.setMontant(dto.getMontant());
        amende.setDateInfraction(dto.getDateInfraction());
        amende.setLieuInfraction(dto.getLieuInfraction());
        amende.setMotif(dto.getMotif());
        amende.setDescription(dto.getDescription());
        amende.setPhoto(dto.getPhoto());
        amende.setStatut(dto.getStatut() != null ? dto.getStatut() : "SIGNALÉE");
        amende.setPayee(dto.getPayee() != null ? dto.getPayee() : false);

        if (dto.getChauffeurId() != null) {
            amende.setChauffeur(chauffeurRepository.findById(dto.getChauffeurId())
                .orElseThrow(() -> new RuntimeException("Chauffeur non trouvé")));
        }
        // ✅ vehiculeId facultatif — pas de NPE si absent
        if (dto.getVehiculeId() != null) {
            amende.setVehicule(vehiculeRepository.findById(dto.getVehiculeId())
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé")));
        }
        return amende;
    }

    /**
     * Résoudre l'ID du chef de parc.
     * Priorité 1 : chef du parc du chauffeur (JOIN FETCH → pas de lazy loading)
     * Priorité 2 : fallback → premier utilisateur avec rôle CHEF en base
     */
    private Long resolveChefParcId(Long chauffeurId) {
        if (chauffeurId != null) {
            try {
                var opt = chauffeurRepository.findByIdWithParcAndChef(chauffeurId);
                if (opt.isPresent()) {
                    Chauffeur c = opt.get();
                    if (c.getParc() != null && c.getParc().getChef() != null) {
                        log.info("Chef résolu via parc du chauffeur : chefId={}", c.getParc().getChef().getId());
                        return c.getParc().getChef().getId();
                    }
                    log.warn("Chauffeur id={} sans parc/chef → fallback", chauffeurId);
                }
            } catch (Exception e) {
                log.error("Erreur résolution chef via parc : {}", e.getMessage());
            }
        }
        // Fallback : chercher n'importe quel chef en base
        Long chefId = notificationService.getChefParcId();
        if (chefId != null) log.info("Chef résolu via fallback rôle CHEF : chefId={}", chefId);
        else log.error("Aucun chef trouvé — notification impossible");
        return chefId;
    }

    // === CRUD ===
    @Transactional
    public AmendeDTO create(AmendeDTO dto) {
        Amende amende = toEntity(dto);
        amende = amendeRepository.save(amende);
        log.info("Amende sauvegardée id={}", amende.getId());

        // ✅ Notification : fonctionne même sans véhicule (NotificationService gère null)
        Long chefId = resolveChefParcId(dto.getChauffeurId());
        if (chefId != null) {
            notificationService.notifierAmendeDeclaree(chefId, amende);
        }

        return toDTO(amende);
    }

    public List<AmendeDTO> getAll() {
        return amendeRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AmendeDTO getById(Long id) {
        return amendeRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Amende non trouvée"));
    }

    public List<AmendeDTO> getByChauffeur(Long chauffeurId) {
        return amendeRepository.findByChauffeurId(chauffeurId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AmendeDTO> getByVehicule(Long vehiculeId) {
        return amendeRepository.findByVehiculeId(vehiculeId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AmendeDTO update(Long id, AmendeDTO dto) {
        Amende existing = amendeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amende non trouvée"));
        existing.setMontant(dto.getMontant());
        existing.setDateInfraction(dto.getDateInfraction());
        existing.setLieuInfraction(dto.getLieuInfraction());
        existing.setMotif(dto.getMotif());
        existing.setDescription(dto.getDescription());
        existing.setPhoto(dto.getPhoto());
        existing.setStatut(dto.getStatut());
        existing.setPayee(dto.getPayee());
        if (dto.getChauffeurId() != null) {
            existing.setChauffeur(chauffeurRepository.findById(dto.getChauffeurId())
                .orElseThrow(() -> new RuntimeException("Chauffeur non trouvé")));
        } else { existing.setChauffeur(null); }
        if (dto.getVehiculeId() != null) {
            existing.setVehicule(vehiculeRepository.findById(dto.getVehiculeId())
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé")));
        } else { existing.setVehicule(null); }
        return toDTO(amendeRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        amendeRepository.delete(amendeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amende non trouvée")));
    }

    @Transactional
    public AmendeDTO marquerPayee(Long id) {
        Amende amende = amendeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amende non trouvée"));
        amende.setPayee(true);
        return toDTO(amendeRepository.save(amende));
    }
}