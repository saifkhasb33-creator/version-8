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
     * Soumet un rapport texte après une maintenance.
     * L'opérateur est identifié par son email.
     *
     * FIX 1 : La vérification du garage est assouplie — si la maintenance ou l'opérateur
     *          n'a pas de garage assigné, on ne bloque plus l'envoi.
     * FIX 2 : Si aucun chef de parc n'est trouvé, on sauvegarde quand même le rapport
     *          et on retourne proprement au lieu de lever une exception.
     */
    @Transactional
    public void soumettreRapportMaintenance(Long maintenanceId, String emailOperateur, String rapportText) throws Exception {
        if (rapportText == null || rapportText.trim().isEmpty()) {
            throw new RuntimeException("Le rapport ne peut pas être vide");
        }

        // Récupérer l'opérateur via son email
        OperateurMaintenance operateur = operateurRepository.findByEmail(emailOperateur)
                .orElseThrow(() -> new RuntimeException("Opérateur maintenance non trouvé avec l'email : " + emailOperateur));

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance non trouvée"));

        // FIX 1 : Vérification assouplie du garage
        // On ne bloque que si les deux garages existent ET sont différents.
        // Si l'un des deux est null, on autorise l'envoi.
        if (maintenance.getGarage() != null && operateur.getGarage() != null &&
                !maintenance.getGarage().getId().equals(operateur.getGarage().getId())) {
            throw new RuntimeException("Cette maintenance n'appartient pas à votre garage");
        }

        // Sauvegarder le rapport dans tous les cas
        maintenance.setRapportProbleme(rapportText);
        maintenanceRepository.save(maintenance);

        // FIX 2 : Si pas de chef de parc, on ne lève plus d'exception —
        // le rapport est déjà sauvegardé, on logue et on sort proprement.
        if (maintenance.getVehicule() == null
                || maintenance.getVehicule().getParc() == null
                || maintenance.getVehicule().getParc().getChef() == null) {
            System.err.println("⚠️ Rapport sauvegardé mais aucun chef de parc trouvé pour la maintenance " + maintenanceId
                    + " — notification non envoyée.");
            return;
        }

        Long chefId = maintenance.getVehicule().getParc().getChef().getId();
        String lienTelechargement = "/chef/rapports-maintenance";
        String matricule = maintenance.getVehicule() != null ? maintenance.getVehicule().getMatricule() : "N/A";
        String message = "Rapport de maintenance soumis pour le véhicule "
                + matricule
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
     *
     * FIX 1 : Vérification garage assouplie (même logique que soumettreRapportMaintenance).
     * FIX 2 : Pas d'exception si aucun chef — logue et retourne proprement.
     */
    @Transactional
    public void envoyerRapportMaintenanceAuChef(Long maintenanceId, String emailOperateur) throws Exception {
        // Récupérer l'opérateur via son email
        OperateurMaintenance operateur = operateurRepository.findByEmail(emailOperateur)
                .orElseThrow(() -> new RuntimeException("Opérateur maintenance non trouvé avec l'email : " + emailOperateur));

        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance non trouvée"));

        // FIX 1 : Vérification assouplie du garage
        if (maintenance.getGarage() != null && operateur.getGarage() != null &&
                !maintenance.getGarage().getId().equals(operateur.getGarage().getId())) {
            throw new RuntimeException("Cette maintenance n'appartient pas à votre garage");
        }

        // FIX 2 : Pas de chef → log + return sans exception
        if (maintenance.getVehicule() == null
                || maintenance.getVehicule().getParc() == null
                || maintenance.getVehicule().getParc().getChef() == null) {
            System.err.println("⚠️ Aucun chef de parc trouvé pour la maintenance " + maintenanceId
                    + " — notification PDF non envoyée.");
            return;
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
     * Téléchargement du PDF du rapport.
     *
     * FIX : peutTelechargerRapport assoupli — si l'opérateur ou la maintenance n'a pas
     *        de garage, on autorise quand même le téléchargement pour l'opérateur concerné.
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

    /**
     * FIX : Pour OPERATEUR_MAINTENANCE, si le garage est null des deux côtés,
     *        on autorise l'accès au lieu de refuser.
     */
    private boolean peutTelechargerRapport(Utilisateur utilisateur, Maintenance maintenance) {
        if (utilisateur.getRole() == Role.CHEF) {
            return maintenance.getVehicule() != null
                    && maintenance.getVehicule().getParc() != null
                    && maintenance.getVehicule().getParc().getChef() != null
                    && maintenance.getVehicule().getParc().getChef().getId().equals(utilisateur.getId());
        }

        if (utilisateur.getRole() == Role.OPERATEUR_MAINTENANCE) {
            OperateurMaintenance operateur = operateurRepository.findById(utilisateur.getId()).orElse(null);
            if (operateur == null) return false;

            // FIX : Si l'un des garages est null, on autorise l'accès
            if (maintenance.getGarage() == null || operateur.getGarage() == null) {
                return true;
            }

            return maintenance.getGarage().getId().equals(operateur.getGarage().getId());
        }

        return false;
    }
}