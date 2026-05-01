package com.parc.service;

import com.parc.dto.AmendeDTO;
import com.parc.dto.ChauffeurDTO;
import com.parc.dto.MaintenanceDTO;
import com.parc.dto.MissionDTO;
import com.parc.dto.VehiculeDTO;
import com.parc.domain.enums.Disponibilite;
import com.parc.domain.enums.StatutMaintenance;
import com.parc.domain.enums.StatutMission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantAIService {

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:mistral}")
    private String ollamaModel;

    private final RestTemplate restTemplate;
    private final StatistiquesService statistiquesService;
    private final VehiculeService vehiculeService;
    private final ChauffeurService chauffeurService;
    private final MissionService missionService;
    private final MaintenanceService maintenanceService;
    private final AmendeService amendeService;
    private final ObjectMapper objectMapper;

    /**
     * Traiter les requêtes utilisateur avec l'IA
     */
    public String processUserQuery(String userMessage, Map<String, Object> context) {
        try {
            String enrichedContext = buildContextFromDatabase(context);
            String prompt = buildPrompt(userMessage, enrichedContext);
            return callOllama(prompt);
        } catch (Exception e) {
            log.error("Erreur traitement query utilisateur", e);
            return "❌ Erreur lors du traitement: " + e.getMessage();
        }
    }

    /**
     * Construire le contexte à partir des vraies données de la base de données
     */
    private String buildContextFromDatabase(Map<String, Object> context) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("\n=== CONTEXTE FLOTTE ===\n");

        try {
            // Statistiques du parc
            if (context != null && context.containsKey("stats")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) context.get("stats");
                ctx.append("📊 STATISTIQUES PARC:\n");
                stats.forEach((key, value) -> {
                    ctx.append("  - ").append(translateStatKey(key)).append(": ").append(value).append("\n");
                });
            }

            // Véhicules en maintenance
            List<MaintenanceDTO> maintenances = maintenanceService.getAll();
            List<MaintenanceDTO> maintenancesEnCours = maintenances.stream()
                    .filter(m -> m.getStatut() == StatutMaintenance.EN_COURS || m.getStatut() == StatutMaintenance.PLANIFIEE)
                    .collect(Collectors.toList());
            ctx.append("\n🔧 MAINTENANCES EN COURS/PLANIFIÉES (").append(maintenancesEnCours.size()).append("):\n");
            maintenancesEnCours.stream().limit(5).forEach(m -> {
                ctx.append("  - Véhicule ").append(m.getVehiculeMatricule())
                   .append(" | Type: ").append(m.getType())
                   .append(" | Date: ").append(m.getDatePrevue())
                   .append(" | Opérateur: ").append(m.getOperateur() != null ? m.getOperateur() : "Non assigné")
                   .append("\n");
            });
            if (maintenancesEnCours.size() > 5) {
                ctx.append("  ... et ").append(maintenancesEnCours.size() - 5).append(" autres\n");
            }

            // Chauffeurs disponibles
            List<ChauffeurDTO> chauffeurs = chauffeurService.getAllChauffeurs();
            List<ChauffeurDTO> chauffeursDisponibles = chauffeurs.stream()
                    .filter(c -> c.getDisponible() == Disponibilite.DISPONIBLE)
                    .collect(Collectors.toList());
            ctx.append("\n👥 CHAUFFEURS DISPONIBLES (").append(chauffeursDisponibles.size()).append("/").append(chauffeurs.size()).append("):\n");
            chauffeursDisponibles.stream().limit(5).forEach(c -> {
                ctx.append("  - ").append(c.getNom()).append(" ").append(c.getPrenom())
                   .append(" | Permis: ").append(c.getNumeroPermis())
                   .append(" | Parc: ").append(c.getParcNom() != null ? c.getParcNom() : "N/A")
                   .append("\n");
            });
            if (chauffeursDisponibles.size() > 5) {
                ctx.append("  ... et ").append(chauffeursDisponibles.size() - 5).append(" autres\n");
            }

            // Missions en cours
            List<MissionDTO> missions = missionService.getAllMissions();
            List<MissionDTO> missionsEnCours = missions.stream()
                    .filter(m -> m.getStatut() == StatutMission.EN_COURS)
                    .collect(Collectors.toList());
            ctx.append("\n📍 MISSIONS EN COURS (").append(missionsEnCours.size()).append("/").append(missions.size()).append("):\n");
            missionsEnCours.stream().limit(5).forEach(m -> {
                ctx.append("  - ").append(m.getDescription())
                   .append(" | Destination: ").append(m.getDestination())
                   .append(" | Véhicule: ").append(m.getVehiculeMatricule() != null ? m.getVehiculeMatricule() : "N/A")
                   .append(" | Chauffeur: ").append(m.getChauffeurNom() != null ? m.getChauffeurNom() : "N/A")
                   .append("\n");
            });
            if (missionsEnCours.size() > 5) {
                ctx.append("  ... et ").append(missionsEnCours.size() - 5).append(" autres\n");
            }

            // Amende récentes
            List<AmendeDTO> amendes = amendeService.getAll();
            double totalAmendes = amendes.stream().mapToDouble(AmendeDTO::getMontant).sum();
            long amendesNonPayees = amendes.stream().filter(a -> !Boolean.TRUE.equals(a.getPayee())).count();
            ctx.append("\n💰 AMENDES:\n");
            ctx.append("  - Total amendes: ").append(String.format("%.2f", totalAmendes)).append(" TND\n");
            ctx.append("  - Amendes non payées: ").append(amendesNonPayees).append("\n");

            // Véhicules - alertes importantes
            List<VehiculeDTO> vehicules = vehiculeService.getAll();
            long vehiculesEnMaintenance = vehicules.stream()
                    .filter(v -> v.getStatut() != null && "EN_MAINTENANCE".equals(v.getStatut().toString()))
                    .count();
            ctx.append("\n🚗 VÉHICULES:\n");
            ctx.append("  - Total véhicules: ").append(vehicules.size()).append("\n");
            ctx.append("  - Véhicules en maintenance: ").append(vehiculesEnMaintenance).append("\n");

        } catch (Exception e) {
            log.warn("Erreur construction contexte DB - certaines données peuvent manquer", e);
            ctx.append("\n⚠️ Note: Certaines données n'ont pas pu être chargées.\n");
        }

        return ctx.toString();
    }

    /**
     * Construire le prompt pour l'IA
     */
    private String buildPrompt(String userMessage, String context) {
        return String.format("""
                Vous êtes un assistant IA spécialisé pour la gestion d'une flotte de véhicules.
                Vous aidez les chefs de parc à prendre des décisions basées sur les données de leur flotte.
                
                Répondez en français, de manière concise et pragmatique.
                Utilisez des emojis pour une meilleure lisibilité.
                
                %s
                
                QUESTION DU CHEF DE PARC: %s
                
                Réponse:""", context, userMessage);
    }

    /**
     * Appeler Ollama pour obtenir la réponse IA
     */
    private String callOllama(String prompt) {
        try {
            log.info("📞 Appel Ollama avec modèle: {}", ollamaModel);

            Map<String, Object> request = new HashMap<>();
            request.put("model", ollamaModel);
            request.put("prompt", prompt);
            request.put("stream", false);
            request.put("temperature", 0.7);

            String ollamaEndpoint = ollamaUrl + "/api/generate";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                ollamaEndpoint,
                request,
                Map.class
            );

            if (response != null && response.containsKey("response")) {
                String aiResponse = (String) response.get("response");
                log.info("✅ Réponse Ollama reçue ({} caractères)", aiResponse.length());
                return aiResponse.trim();
            }

            return "❌ Pas de réponse de l'IA";

        } catch (RestClientException e) {
            log.error("❌ Erreur connexion Ollama", e);
            throw new RuntimeException(
                "Impossible de se connecter à Ollama. Assurez-vous qu'Ollama est lancé sur " + ollamaUrl
            );
        }
    }

    /**
     * Vérifier si Ollama est connecté
     */
    public boolean isOllamaConnected() {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                ollamaUrl + "/api/tags",
                Map.class
            );
            return response != null;
        } catch (Exception e) {
            log.warn("❌ Ollama non accessible", e);
            return false;
        }
    }

    /**
     * Traduire les clés de statistiques en français
     */
    private String translateStatKey(String key) {
        return switch (key) {
            case "totalChauffeurs" -> "Total Chauffeurs";
            case "chauffeursDisponibles" -> "Chauffeurs Disponibles";
            case "chauffeursOccupees" -> "Chauffeurs Occupés";
            case "chauffeursConges" -> "Chauffeurs en Congé";
            case "totalVehicules" -> "Total Véhicules";
            case "vehiculesDisponibles" -> "Véhicules Disponibles";
            case "vehiculesMaintenance" -> "Véhicules en Maintenance";
            case "totalMissions" -> "Total Missions";
            case "missionsEnCours" -> "Missions en Cours";
            case "missionsTerminees" -> "Missions Terminées";
            default -> key;
        };
    }
}
