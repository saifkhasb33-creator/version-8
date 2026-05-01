package com.parc.controller;

import com.parc.dto.AssistantRequest;
import com.parc.dto.AssistantResponse;
import com.parc.service.AssistantAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssistantController {

    private final AssistantAIService assistantService;

    /**
     * Endpoint pour interroger l'assistant IA
     */
    @PostMapping("/ask")
    public ResponseEntity<AssistantResponse> askAssistant(@RequestBody AssistantRequest request) {
        try {
            if (!assistantService.isOllamaConnected()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AssistantResponse("❌ Ollama est hors ligne. Veuillez le démarrer.", false));
            }
            String response = assistantService.processUserQuery(
                request.getMessage(),
                request.getContext()
            );
            return ResponseEntity.ok(new AssistantResponse(response, true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new AssistantResponse("❌ Erreur: " + e.getMessage(), false));
        }
    }

    /**
     * Vérifier la connexion Ollama
     */
    @GetMapping("/health")
    public ResponseEntity<AssistantResponse> checkHealth() {
        boolean connected = assistantService.isOllamaConnected();
        String message = connected ? "✅ Ollama Connecté" : "❌ Ollama Hors Ligne";
        HttpStatus status = connected ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(new AssistantResponse(message, connected));
    }

    /**
     * Liste des modèles disponibles sur Ollama
     */
    @GetMapping("/models")
    public ResponseEntity<?> getModels() {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("name", "mistral");
            model.put("description", "Modèle par défaut");
            
            Map<String, Object> result = new HashMap<>();
            result.put("models", Arrays.asList(model));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new AssistantResponse("❌ Impossible de lister les modèles: " + e.getMessage(), false));
        }
    }
}
