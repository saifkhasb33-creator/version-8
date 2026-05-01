package com.parc.service;

import com.parc.domain.enums.Role;
import com.parc.domain.enums.StatutMaintenance;
import com.parc.domain.enums.TypeNotification;
import com.parc.dto.MaintenanceDTO;
import com.parc.dto.OperateurMaintenanceDTO;
import com.parc.domain.entity.*;
import com.parc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperateurMaintenanceService {

    private final OperateurMaintenanceRepository operateurRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final GarageRepository garageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final PdfGenerationService pdfGenerationService;

    // ... (toutes les autres méthodes existantes : toDTO, toEntity, create, getAll, getById, update, delete,
    // consulterDemandesMaintenance, validerMaintenance, planifierMaintenance, realiserMaintenance,
    // rapporterProbleme, consulterHistoriqueMaintenance, getMaintenanceForOperateur, findByEmail, getByDisponibilite, etc.)

    /**
     * Soumet un rapport texte (optionnel) après une maintenance.
     * L'opérateur est identifié par son email.
     */
    @Transactional
    public void soumettreRapportMaintenance(Long maintenanceId, String emailOperateur, String rapportText) throws Exception {
        if (rapportText == null || rapportText.trim().isEmpty()) {
            throw new RuntimeException("Le rapport ne peut pas être vide");
        }

        // ✅ Récupérer l'opérateur directement via son email dans la table operateurs_maintenance
        OperateurMaintenance operateur = operateurRepository.findByEmail(emailOperateur)
                .orElseThrow(() -> new RuntimeException("Opérateur maintenance non trouvé avec l'email : " + emailOperateur));

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance non trouvée"));

        // Vérifier que la maintenance appartient bien au garage de l'opérateur
        if (maintenance.getGarage() == null || operateur.getGarage() == null ||
                !maintenance.getGarage().getId().equals(operateur.getGarage().getId())) {
            throw new RuntimeException("Cette maintenance n'appartient pas à votre garage");
        }

        // Mettre à jour le rapport
        maintenance.setRapportProbleme(rapportText);
        maintenanceRepository.save(maintenance);

        if (maintenance.getVehicule() == null || maintenance.getVehicule().getParc() == null
                || maintenance.getVehicule().getParc().getChef() == null) {
            throw new RuntimeException("Aucun chef de parc associé à cette maintenance");
        }

        Long chefId = maintenance.getVehicule().getParc().getChef().getId();
        String lienTelechargement = "/chef/rapports-maintenance";
        String message = "Rapport de maintenance soumis pour le véhicule "
                + (maintenance.getVehicule() != null ? maintenance.getVehicule().getMatricule() : "N/A")
                + " par " + operateur.getNom() + " " + operateur.getPrenom()
                + ". Cliquez pour consulter.";

        notificationService.envoyerNotification(
                chefId,
                "📄 Rapport de maintenance soumis",
                message,
                TypeNotification.RAPPORT_MAINTENANCE,
                lienTelechargement,
                null,
                maintenanceId
        );
    }

    /**
     * Envoie une notification au chef de parc pour signaler qu'un rapport PDF est disponible.
     */
    @Transactional
    public void envoyerRapportMaintenanceAuChef(Long maintenanceId, String emailOperateur) throws Exception {
        // ✅ Récupérer l'opérateur directement via son email
        OperateurMaintenance operateur = operateurRepository.findByEmail(emailOperateur)
                .orElseThrow(() -> new RuntimeException("Opérateur maintenance non trouvé avec l'email : " + emailOperateur));

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance non trouvée"));

        // Vérifier l'appartenance au garage
        if (maintenance.getGarage() == null || operateur.getGarage() == null ||
                !maintenance.getGarage().getId().equals(operateur.getGarage().getId())) {
            throw new RuntimeException("Cette maintenance n'appartient pas à votre garage");
        }

        if (maintenance.getVehicule() == null || maintenance.getVehicule().getParc() == null
                || maintenance.getVehicule().getParc().getChef() == null) {
            throw new RuntimeException("Aucun chef de parc associé à cette maintenance");
        }

        Long chefId = maintenance.getVehicule().getParc().getChef().getId();
        String lienTelechargement = "/chef/rapports-maintenance";
        String message = "Rapport PDF de maintenance disponible pour le véhicule "
                + maintenance.getVehicule().getMatricule()
                + ". Cliquez pour télécharger.";

        notificationService.envoyerNotification(
                chefId,
                "Nouveau rapport maintenance PDF",
                message,
                TypeNotification.RAPPORT_MAINTENANCE,
                lienTelechargement,
                null,
                maintenanceId
        );
    }

    /**
     * Téléchargement du PDF du rapport (inchangé)
     */
    public byte[] telechargerRapportMaintenancePdf(Long maintenanceId, String emailUtilisateur) throws Exception {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(emailUtilisateur)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance non trouvée"));

        if (!peutTelechargerRapport(utilisateur, maintenance)) {
            throw new RuntimeException("Accès refusé à ce rapport");
        }

        return pdfGenerationService.generateMaintenanceReportPdf(maintenance, utilisateur);
    }

    private boolean peutTelechargerRapport(Utilisateur utilisateur, Maintenance maintenance) {
        if (utilisateur.getRole() == Role.CHEF) {
            return maintenance.getVehicule() != null
                    && maintenance.getVehicule().getParc() != null
                    && maintenance.getVehicule().getParc().getChef() != null
                    && maintenance.getVehicule().getParc().getChef().getId().equals(utilisateur.getId());
        }

        if (utilisateur.getRole() == Role.OPERATEUR_MAINTENANCE) {
            OperateurMaintenance operateur = operateurRepository.findById(utilisateur.getId()).orElse(null);
            return operateur != null
                    && maintenance.getGarage() != null
                    && operateur.getGarage() != null
                    && maintenance.getGarage().getId().equals(operateur.getGarage().getId());
        }

        return false;
    }
}