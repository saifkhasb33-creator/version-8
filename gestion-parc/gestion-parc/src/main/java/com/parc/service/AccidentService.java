package com.parc.service;

import com.parc.dto.AccidentDTO;
import com.parc.domain.entity.Accident;
import com.parc.domain.entity.Chauffeur;
import com.parc.domain.entity.Vehicule;
import com.parc.dto.MaintenanceDTO;
import com.parc.domain.enums.StatutMaintenance;
import com.parc.domain.entity.Mission;
import com.parc.repository.AccidentRepository;
import com.parc.repository.ChauffeurRepository;
import com.parc.repository.VehiculeRepository;
import com.parc.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccidentService {

    private final AccidentRepository accidentRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final MissionRepository missionRepository;
    private final MaintenanceService maintenanceService;
    private final NotificationService notificationService;

    // ========== MAPPING ==========
    private AccidentDTO toDTO(Accident accident) {
        AccidentDTO dto = new AccidentDTO();
        dto.setId(accident.getId());
        dto.setDateAccident(accident.getDateAccident());
        dto.setLieuAccident(accident.getLieuAccident());
        dto.setPersonnesImpliquees(accident.getPersonnesImpliquees());
        dto.setDescription(accident.getDescription());
        dto.setDegats(accident.getDegats());
        dto.setPhoto(accident.getPhoto());
        dto.setStatut(accident.getStatut());
        dto.setDateDeclaration(accident.getDateDeclaration());
        if (accident.getMission() != null) dto.setMissionId(accident.getMission().getId());
        if (accident.getChauffeur() != null) {
            dto.setChauffeurId(accident.getChauffeur().getId());
            dto.setChauffeurNom(accident.getChauffeur().getNom() + " " + accident.getChauffeur().getPrenom());
        }
        if (accident.getVehicule() != null) {
            dto.setVehiculeId(accident.getVehicule().getId());
            dto.setVehiculeMatricule(accident.getVehicule().getMatricule());
        }
        return dto;
    }

    private Accident toEntity(AccidentDTO dto) {
        Accident accident = new Accident();
        accident.setDateAccident(dto.getDateAccident());
        accident.setLieuAccident(dto.getLieuAccident());
        accident.setPersonnesImpliquees(dto.getPersonnesImpliquees());
        accident.setDescription(dto.getDescription());
        accident.setDegats(dto.getDegats());
        accident.setPhoto(dto.getPhoto());
        accident.setStatut(dto.getStatut() != null ? dto.getStatut() : "DÉCLARÉ");

        if (dto.getMissionId() != null) {
            accident.setMission(missionRepository.findById(dto.getMissionId())
                .orElseThrow(() -> new RuntimeException("Mission non trouvée : " + dto.getMissionId())));
        }
        if (dto.getChauffeurId() != null) {
            accident.setChauffeur(chauffeurRepository.findById(dto.getChauffeurId())
                .orElseThrow(() -> new RuntimeException("Chauffeur non trouvé : " + dto.getChauffeurId())));
        }
        if (dto.getVehiculeId() != null) {
            accident.setVehicule(vehiculeRepository.findById(dto.getVehiculeId())
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + dto.getVehiculeId())));
        }
        return accident;
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
        Long chefId = notificationService.getChefParcId();
        if (chefId != null) log.info("Chef résolu via fallback rôle CHEF : chefId={}", chefId);
        else log.error("Aucun chef trouvé — notification impossible");
        return chefId;
    }

    // ========== CRUD ==========
    @Transactional
    public AccidentDTO create(AccidentDTO dto) {
        Accident accident = toEntity(dto);
        accident = accidentRepository.save(accident);
        log.info("Accident sauvegardé id={}", accident.getId());

        // Notification chef de parc
        Long chefId = resolveChefParcId(dto.getChauffeurId());
        if (chefId != null) {
            // ✅ Si pas de véhicule, adapter le message pour éviter NPE dans notifierAccidentDeclare
            if (accident.getVehicule() != null) {
                notificationService.notifierAccidentDeclare(chefId, accident);
            } else {
                // Notification générique sans matricule véhicule
                String nomChauffeur = accident.getChauffeur() != null
                    ? accident.getChauffeur().getNom() + " " + accident.getChauffeur().getPrenom()
                    : "Chauffeur inconnu";
                notificationService.envoyerNotification(
                    chefId,
                    "Accident déclaré",
                    "Accident déclaré par " + nomChauffeur +
                    " - Lieu : " + accident.getLieuAccident(),
                    com.parc.domain.enums.TypeNotification.ACCIDENT_DECLARE,
                    "/accidents/" + accident.getId(),
                    null, null
                );
            }
        }

        // ✅ Maintenance corrective UNIQUEMENT si un véhicule est associé
        if (accident.getVehicule() != null) {
            MaintenanceDTO maintenanceDTO = new MaintenanceDTO();
            maintenanceDTO.setType("Accident");
            maintenanceDTO.setStatut(StatutMaintenance.PLANIFIEE);
            maintenanceDTO.setDatePrevue(LocalDate.now().plusDays(2));
            maintenanceDTO.setOperateur("À assigner");
            maintenanceDTO.setVehiculeId(accident.getVehicule().getId());
            maintenanceService.create(maintenanceDTO);
        }

        return toDTO(accident);
    }

    public List<AccidentDTO> getAll() {
        return accidentRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AccidentDTO getById(Long id) {
        return accidentRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Accident non trouvé : " + id));
    }

    public List<AccidentDTO> getByChauffeur(Long chauffeurId) {
        return accidentRepository.findByChauffeurId(chauffeurId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AccidentDTO> getByVehicule(Long vehiculeId) {
        return accidentRepository.findByVehiculeId(vehiculeId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AccidentDTO update(Long id, AccidentDTO dto) {
        Accident existing = accidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Accident non trouvé : " + id));
        existing.setDateAccident(dto.getDateAccident());
        existing.setLieuAccident(dto.getLieuAccident());
        existing.setPersonnesImpliquees(dto.getPersonnesImpliquees());
        existing.setDescription(dto.getDescription());
        existing.setDegats(dto.getDegats());
        existing.setPhoto(dto.getPhoto());
        existing.setStatut(dto.getStatut());
        if (dto.getMissionId() != null) {
            existing.setMission(missionRepository.findById(dto.getMissionId()).orElseThrow(() -> new RuntimeException("Mission non trouvée")));
        } else { existing.setMission(null); }
        if (dto.getChauffeurId() != null) {
            existing.setChauffeur(chauffeurRepository.findById(dto.getChauffeurId()).orElseThrow(() -> new RuntimeException("Chauffeur non trouvé")));
        } else { existing.setChauffeur(null); }
        if (dto.getVehiculeId() != null) {
            existing.setVehicule(vehiculeRepository.findById(dto.getVehiculeId()).orElseThrow(() -> new RuntimeException("Véhicule non trouvé")));
        } else { existing.setVehicule(null); }
        return toDTO(accidentRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        accidentRepository.delete(accidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Accident non trouvé : " + id)));
    }
}