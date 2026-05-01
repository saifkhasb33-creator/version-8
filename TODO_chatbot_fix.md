# TODO - Corrections ChatBot IA & Flux Rapport Maintenance PDF

## ChatBot IA (corrections validées)
- [x] `frontend/src/services/ollamaService.js` — Health check via backend `/api/assistant/health`
- [x] `frontend/src/components/chef/ChefAssistant.js` — `showSuccess` retiré au montage
- [x] `gestion-parc/.../service/AssistantAIService.java` — `buildContextFromDatabase()` avec vraies données
- [x] `gestion-parc/.../controller/AssistantController.java` — Retourne HTTP 503 si Ollama offline
- [x] `frontend/src/styles/chatbot.css` — Styles obsolètes `.chatbot-*` supprimés

## Flux Rapport Maintenance PDF (implémenté)
- [x] `TypeNotification.java` — Ajout `RAPPORT_MAINTENANCE`
- [x] `OperateurMaintenanceService.java` — Méthode `soumettreRapportMaintenance()` avec texte + notification
- [x] `OperateurMaintenanceController.java` — Endpoints `soumettre-rapport` et `rapport-pdf`
- [x] `frontend/src/services/maintenance.js` — `soumettreRapportMaintenance()` ajouté
- [x] `OperateurMaintenanceList.js` — Modal de rédaction de rapport avec textarea
- [x] `chef.css` — Styles du modal `.modal-overlay`, `.modal-content`, `.report-textarea`
- [x] `NotificationPanel.js` — Gestion `RAPPORT_MAINTENANCE` avec redirection vers `/chef/rapports-maintenance`
- [x] `ChefMaintenanceReports.js` — Page chef pour télécharger les PDF

## Tests de compilation
- [x] Backend Spring Boot : `mvn compile` ✅ SUCCESS
- [x] Frontend React : `npm run build` ✅ SUCCESS (warnings ESLint existants non bloquants)

