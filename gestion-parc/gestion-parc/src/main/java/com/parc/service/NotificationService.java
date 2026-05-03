package com.parc.service;

import com.parc.domain.entity.*;
import com.parc.domain.enums.StatutNotification;
import com.parc.domain.enums.TypeNotification;
import com.parc.dto.NotificationDTO;
import com.parc.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MissionRepository missionRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final SmsService smsService;
    private final PushNotificationService pushService;

    // Conversion DTO
    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setTitre(n.getTitre());
        dto.setMessage(n.getMessage());
        dto.setLien(n.getLien());
        dto.setType(n.getType());
        dto.setStatut(n.getStatut().name());
        dto.setDateEnvoi(n.getDateEnvoi());
        if (n.getDestinataire() != null) {
            dto.setDestinataireId(n.getDestinataire().getId());
            dto.setDestinataireNom(n.getDestinataire().getNom() + " " + n.getDestinataire().getPrenom());
        }
        if (n.getMission() != null) {
            dto.setMissionId(n.getMission().getId());
            dto.setMissionDestination(n.getMission().getDestination());
            dto.setMissionDateDebut(n.getMission().getDateDebut());
            dto.setMissionDateFin(n.getMission().getDateFin());
            dto.setMissionStatut(n.getMission().getStatut() != null ? n.getMission().getStatut().name() : "");
            dto.setMissionDescription(n.getMission().getDescription());
        }
        if (n.getMaintenance() != null) dto.setMaintenanceId(n.getMaintenance().getId());
        return dto;
    }

    // Récupérer l'ID du premier chef de parc (ou null si inexistant)
    public Long getChefParcId() {
        List<Utilisateur> chefs = utilisateurRepository.findByRole("CHEF");
        if (chefs.isEmpty()) {
            log.error("❌ Aucun chef de parc trouvé avec le rôle CHEF !");
            return null;
        }
        log.info("✅ Chef de parc trouvé: id={}, nom={}", chefs.get(0).getId(), chefs.get(0).getNom());
        return chefs.get(0).getId();
    }

    // Méthode générique d'envoi
    @Transactional
    public NotificationDTO envoyerNotification(Long destinataireId, String titre, String message,
                                                TypeNotification type, String lien,
                                                Long missionId, Long maintenanceId) {
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Notification notif = new Notification();
        notif.setTitre(titre);
        notif.setMessage(message);
        notif.setLien(lien);
        notif.setType(type);
        notif.setDestinataire(destinataire);
        notif.setStatut(StatutNotification.NON_LUE);
        notif.setDateEnvoi(LocalDateTime.now());

        if (missionId != null) {
            missionRepository.findById(missionId).ifPresent(notif::setMission);
        }
        if (maintenanceId != null) {
            maintenanceRepository.findById(maintenanceId).ifPresent(notif::setMaintenance);
        }

        Notification saved = notificationRepository.save(notif);
        log.info("📨 Notification enregistrée (id={}) pour l'utilisateur {} : {}", saved.getId(), destinataireId, titre);

        if (destinataire instanceof Chauffeur && ((Chauffeur) destinataire).getFcmToken() != null) {
            pushService.sendPush(((Chauffeur) destinataire).getFcmToken(), titre, message);
        }

        return toDTO(saved);
    }

    // ✅ Notifications accidents — gère le cas sans véhicule
    public void notifierAccidentDeclare(Long chefParcId, Accident accident) {
        if (chefParcId == null) {
            log.warn("⚠️ Notification accident ignorée : chefParcId null");
            return;
        }
        String nomChauffeur = accident.getChauffeur() != null
            ? accident.getChauffeur().getNom() + " " + accident.getChauffeur().getPrenom()
            : "Chauffeur inconnu";
        // ✅ getMatricule() uniquement si véhicule présent
        String vehiculeInfo = (accident.getVehicule() != null)
            ? "véhicule " + accident.getVehicule().getMatricule()
            : "véhicule non renseigné";

        log.info("📨 Envoi notification accident au chef {} pour accident {}", chefParcId, accident.getId());
        envoyerNotification(chefParcId,
            "🚨 Accident déclaré",
            "Accident déclaré (" + vehiculeInfo + ") par " + nomChauffeur +
            " - Lieu : " + accident.getLieuAccident(),
            TypeNotification.ACCIDENT_DECLARE,
            "/accidents/" + accident.getId(),
            null, null);
    }

    // ✅ Notifications amendes — gère le cas sans véhicule
    public void notifierAmendeDeclaree(Long chefParcId, Amende amende) {
        if (chefParcId == null) {
            log.warn("⚠️ Notification amende ignorée : chefParcId null");
            return;
        }
        String nomChauffeur = amende.getChauffeur() != null
            ? amende.getChauffeur().getNom() + " " + amende.getChauffeur().getPrenom()
            : "Chauffeur inconnu";
        // ✅ getMatricule() uniquement si véhicule présent
        String vehiculeInfo = (amende.getVehicule() != null)
            ? "véhicule " + amende.getVehicule().getMatricule()
            : "véhicule non renseigné";

        log.info("📨 Envoi notification amende au chef {} pour amende {}", chefParcId, amende.getId());
        envoyerNotification(chefParcId,
            "⚠️ Amende déclarée",
            "Amende déclarée par " + nomChauffeur +
            " (" + vehiculeInfo + ")" +
            " - Montant : " + amende.getMontant() + " TND" +
            " - Motif : " + amende.getMotif(),
            TypeNotification.AMENDE_DECLAREE,
            "/amendes/" + amende.getId(),
            null, null);
    }

    // Notifications pour missions
    public void notifierMissionAssignee(Long chauffeurId, Mission mission) {
        envoyerNotification(chauffeurId,
            "Nouvelle mission",
            "Une nouvelle mission vous a été assignée : " + mission.getDestination(),
            TypeNotification.MISSION_ASSIGNEE,
            "/missions/" + mission.getId(),
            mission.getId(), null);
    }

    public void notifierMissionModifiee(Long chauffeurId, Mission mission) {
        envoyerNotification(chauffeurId,
            "Mission modifiée",
            "La mission vers " + mission.getDestination() + " a été modifiée",
            TypeNotification.MISSION_MODIFIEE,
            "/missions/" + mission.getId(),
            mission.getId(), null);
    }

    public void notifierMissionAnnulee(Long chauffeurId, Mission mission) {
        envoyerNotification(chauffeurId,
            "Mission annulée",
            "La mission vers " + mission.getDestination() + " a été annulée",
            TypeNotification.MISSION_ANNULEE,
            "/missions",
            mission.getId(), null);
    }

    // Notifications pour maintenance
    public void notifierMaintenanceProgrammee(Long garageId, Maintenance maintenance) {
        Garage garage = maintenance.getGarage();
        if (garage != null && garage.getFcmToken() != null) {
            pushService.sendPush(garage.getFcmToken(),
                "Maintenance programmée",
                "Une maintenance est prévue pour le " + maintenance.getDatePrevue());
        }
        if (maintenance.getVehicule() != null && maintenance.getVehicule().getParc() != null) {
            envoyerNotification(maintenance.getVehicule().getParc().getChef().getId(),
                "Maintenance programmée",
                "Le véhicule " + maintenance.getVehicule().getMatricule() + " sera en maintenance le " + maintenance.getDatePrevue(),
                TypeNotification.MAINTENANCE_PROGRAMMEE,
                "/maintenances/" + maintenance.getId(),
                null, maintenance.getId());
        }
    }

    public void notifierMaintenanceImminente(Long maintenanceId) {
        Maintenance m = maintenanceRepository.findById(maintenanceId).orElse(null);
        if (m == null) return;
        if (m.getGarage() != null && m.getGarage().getFcmToken() != null) {
            pushService.sendPush(m.getGarage().getFcmToken(),
                "Maintenance imminente",
                "La maintenance prévue le " + m.getDatePrevue() + " approche");
        }
    }

    // Alertes expiration
    public void alerterExpirationAssurance(Vehicule vehicule) {
        envoyerNotification(vehicule.getParc().getChef().getId(),
            "Expiration assurance imminente",
            "L'assurance du véhicule " + vehicule.getMatricule() + " expire le " + vehicule.getDateExpirationAssurance(),
            TypeNotification.EXPIRATION_ASSURANCE,
            "/vehicules/" + vehicule.getId(),
            null, null);
    }

    public void alerterExpirationCarteGrise(Vehicule vehicule) {
        envoyerNotification(vehicule.getParc().getChef().getId(),
            "Expiration carte grise imminente",
            "La carte grise du véhicule " + vehicule.getMatricule() + " expire le " + vehicule.getDateExpirationCarteGrise(),
            TypeNotification.EXPIRATION_CARTE_GRISE,
            "/vehicules/" + vehicule.getId(),
            null, null);
    }

    public void alerterExpirationVisiteTechnique(Vehicule vehicule) {
        envoyerNotification(vehicule.getParc().getChef().getId(),
            "Expiration visite technique imminente",
            "La visite technique du véhicule " + vehicule.getMatricule() + " expire le " + vehicule.getDateExpirationVisiteTechnique(),
            TypeNotification.EXPIRATION_VISITE_TECHNIQUE,
            "/vehicules/" + vehicule.getId(),
            null, null);
    }

    public void alerterDepassementConsommation(Vehicule vehicule, double consommation, double seuil) {
        envoyerNotification(vehicule.getParc().getChef().getId(),
            "Dépassement de consommation",
            "Le véhicule " + vehicule.getMatricule() + " a une consommation anormale : " + consommation + " L/100km (seuil: " + seuil + ")",
            TypeNotification.DEPASSEMENT_CONSOMMATION,
            "/vehicules/" + vehicule.getId() + "/consommation",
            null, null);
    }

    // Notifications congés
    public void notifierNouvelleDemandeConge(Long chefParcId, Conge conge) {
        envoyerNotification(chefParcId,
            "Nouvelle demande de congé",
            "Le chauffeur " + conge.getChauffeur().getNom() + " " + conge.getChauffeur().getPrenom() +
            " a demandé un congé du " + conge.getDateDebut() + " au " + conge.getDateFin(),
            TypeNotification.CONGE_DEMANDE,
            "/conges/" + conge.getId(),
            null, null);
    }

    public void notifierCongeApprouve(Long chauffeurId, Conge conge) {
        envoyerNotification(chauffeurId,
            "Congé approuvé",
            "Votre demande de congé du " + conge.getDateDebut() + " au " + conge.getDateFin() + " a été approuvée",
            TypeNotification.CONGE_APPROUVE,
            "/conges",
            null, null);
    }

    public void notifierCongeRefuse(Long chauffeurId, Conge conge, String motif) {
        envoyerNotification(chauffeurId,
            "Congé refusé",
            "Votre demande de congé du " + conge.getDateDebut() + " au " + conge.getDateFin() +
            " a été refusée. Motif : " + motif,
            TypeNotification.CONGE_REFUSE,
            "/conges",
            null, null);
    }

    public void envoyerNotificationChauffeur(Long chauffeurId, String message) {
        envoyerNotification(chauffeurId, "Notification", message, TypeNotification.ALERTE_GENERALE, null, null, null);
    }

    public void envoyerNotificationChefParc(String message) {
        Long chefId = getChefParcId();
        if (chefId != null) {
            envoyerNotification(chefId, "Notification", message, TypeNotification.ALERTE_GENERALE, null, null, null);
        }
    }

    // Récupération des notifications
    public List<NotificationDTO> getMesNotifications(Long utilisateurId) {
        return notificationRepository.findByDestinataireIdWithRelations(utilisateurId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public long getNonLuesCount(Long utilisateurId) {
        return notificationRepository.countByDestinataireIdAndStatut(utilisateurId, StatutNotification.NON_LUE);
    }

    @Transactional
    public void marquerCommeLue(Long notificationId) {
        notificationRepository.updateStatut(notificationId, StatutNotification.LUE);
    }

    @Transactional
    public void marquerToutesCommeLues(Long utilisateurId) {
        List<Notification> notifications = notificationRepository.findByDestinataireIdAndStatutOrderByDateEnvoiDesc(
                utilisateurId, StatutNotification.NON_LUE);
        notifications.forEach(n -> n.setStatut(StatutNotification.LUE));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void deleteAllNotifications(Long utilisateurId) {
        List<Notification> notifications = notificationRepository.findByDestinataireIdOrderByDateEnvoiDesc(utilisateurId);
        notificationRepository.deleteAll(notifications);
    }
}