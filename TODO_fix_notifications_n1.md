# TODO - Correction N+1 Notifications

## Problèmes identifiés
1. Requête utilisateur dupliquée (JwtFilter + NotificationController.getUserIdByEmail)
2. N+1 sur notifications : pour chaque notification, requêtes supplémentaires pour destinataire, mission, maintenance

## Étapes de correction

- [x] 1. Créer `security/CustomUserDetails.java` avec champ `id`
- [x] 2. Modifier `UtilisateurService.loadUserByUsername()` → retourner `CustomUserDetails`
- [x] 3. Modifier `NotificationController` → cast `CustomUserDetails` pour obtenir `id`
- [x] 4. Ajouter `JOIN FETCH` dans `NotificationRepository`
- [x] 5. Modifier `NotificationService.getMesNotifications()` → utiliser la méthode repo optimisée
- [ ] 6. Tester et vérifier les logs

