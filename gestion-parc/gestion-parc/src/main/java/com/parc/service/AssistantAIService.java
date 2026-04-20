package com.parc.service;

import com.parc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantAIService {

    // Configuration Ollama
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
            // Construire le contexte enrichi avec les données de la base de données
            String enrichedContext = buildContextFromDatabase(context);

            // Créer le prompt avec contexte
            String prompt = buildPrompt(userMessage, enrichedContext);

            // Appeler Ollama
            String aiResponse = callOllama(prompt);

            return aiResponse;
        } catch (Exception e) {
            log.error("Erreur traitement query utilisateur", e);
            return "❌ Erreur lors du traitement: " + e.getMessage();
        }
    }

    /**
     * Construire le contexte à partir de la base de données
     */
    private String buildContextFromDatabase(Map<String, Object> context) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("\n=== CONTEXTE FLOTTE ===\n");

        try {
            // Récupérer les statistiques du parc
            if (context.containsKey("stats")) {
                Map<String, Object> stats = (Map<String, Object>) context.get("stats");
                contextBuilder.append("📊 STATISTIQUES PARC:\n");
                stats.forEach((key, value) -> {
                    String frenchKey = translateStatKey(key);
                    contextBuilder.append("  - ").append(frenchKey).append(": ").append(value).append("\n");
                });
            }

            // Ajouter les véhicules en maintenance
            contextBuilder.append("\n🔧 MAINTENANCE:\n");
            contextBuilder.append("  Les véhicules en maintenance doivent être revisités régulièrement\n");

            // Ajouter les conducteurs disponibles
            contextBuilder.append("\n👥 CONDUCTEURS:\n");
            contextBuilder.append("  Consultez la liste pour voir les conducteurs disponibles\n");

            // Ajouter les missions en cours
            contextBuilder.append("\n📍 MISSIONS:\n");
            contextBuilder.append("  Suivez l'état des missions en temps réel\n");

        } catch (Exception e) {
            log.warn("Erreur construction contexte DB", e);
        }

        return contextBuilder.toString();
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

            // Appeler Ollama
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                ollamaEndpoint,
                request,
                Map.class
            );

            if (response != null && response.containsKey("response")) {
                String aiResponse = (String) response.get("response");
                log.info("✅ Réponse Ollama reçue");
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
