package com.parc.controller;

import com.parc.security.CustomUserDetails;
import com.parc.service.OperateurMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operateurs-maintenance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OperateurMaintenanceController {

    private final OperateurMaintenanceService operateurMaintenanceService;

    /**
     * Soumettre un rapport de maintenance (texte) et notifier le chef de parc
     */
    @PostMapping("/maintenances/{maintenanceId}/soumettre-rapport")
    public ResponseEntity<?> soumettreRapport(
            @PathVariable Long maintenanceId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String rapportText = body.get("rapportText");
            if (rapportText == null || rapportText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le rapport ne peut pas etre vide"));
            }

            String emailOperateur = userDetails.getUsername();
            operateurMaintenanceService.soumettreRapportMaintenance(maintenanceId, emailOperateur, rapportText);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rapport soumis avec succes",
                "maintenanceId", maintenanceId
            ));

        } catch (RuntimeException e) {
            System.err.println("⚠️ Erreur métier soumission rapport: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue soumission rapport: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur lors de la soumission du rapport: " + e.getMessage()));
        }
    }

    /**
     * Télécharger le rapport PDF d'une maintenance
     */
    @GetMapping("/maintenances/{maintenanceId}/rapport-pdf")
    public ResponseEntity<byte[]> telechargerRapportPdf(
            @PathVariable Long maintenanceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String emailUtilisateur = userDetails.getUsername();
            byte[] pdfBytes = operateurMaintenanceService.telechargerRapportMaintenancePdf(maintenanceId, emailUtilisateur);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("rapport-maintenance-" + maintenanceId + ".pdf")
                .build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Erreur generation PDF: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Endpoint pour envoyer une notification rapport sans texte au chef
     */
    @PostMapping("/maintenances/{maintenanceId}/envoyer-rapport-chef")
    public ResponseEntity<?> envoyerRapportChef(
            @PathVariable Long maintenanceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String emailOperateur = userDetails.getUsername();
            operateurMaintenanceService.envoyerRapportMaintenanceAuChef(maintenanceId, emailOperateur);
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification envoyee au chef"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}

