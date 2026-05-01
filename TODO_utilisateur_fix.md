# Plan de Correction - Gestion Utilisateurs

## Résumé de l'Analyse

### Structure actuelle:
1. **Frontend (UserForm.js)** → API → **Controller** → **Service** → **Base table + Role-specific table**

### Problèmes identifiés:

1. **UtilisateurService.update()** - Ne synchronise pas les tables rôle-specifiques
   - Quand on modifie un CHAUFFEUR, les champs comme `numeroPermis`, `dateExpirationPermis`, `disponible`, `parc` ne sont PAS mis à jour dans la table `chauffeurs`
   - Pareil pour CHEF et OPERATEUR_MAINTENANCE

2. **UtilisateurMapper.toDTO()** - Champs manquants
   - `parcNom` est affiché dans UserList mais n'existe pas dans le DTO
   - Les champs rôle-spécifiques ne sont pas mappés

3. **Service.create()** - Incomplet pour certains champs
   - Certains champs ne sont pas sauvegardés correctement

## Fichiers à modifier:

### 1. UtilisateurMapper.java
Ajouter le mapping des champs rôle-spécifiques:
- Chauffeur: `numeroPermis`, `dateExpirationPermis`, `disponible`, `parcNom`
- ChefDeParc: `dateEmbauche`, `zoneAffectation`, `parcNom`
- OperateurMaintenance: `specialite`, `niveau`, `dateEmbauche`, `garageNom`

### 2. UtilisateurService.java
Corriger la méthode `update()` pour:
- Synchroniser les champs dans les tables rôle-spécifiques
- Gérer le changement de rôle (supprimer l'ancien enregistrement rôle-spécifique si changement)

### 3. UtilisateurDTO.java
Ajouter les champs:
- `parcNom` (String) - nom du parc affecté
- `garageNom` (String) - nom du garagepour OperateurMaintenance

## Détails des corrections:

### A. UtilisateurMapper.java modifications:

```java
// Dans toDTO(Utilisateur user)
// Ajouter les champs rôle-spécifiques après le mapping de base

// Pour Chauffeur
if (user instanceof Chauffeur) {
    Chauffeur c = (Chauffeur) user;
    dto.setNumeroPermis(c.getNumeroPermis());
    dto.setDateExpirationPermis(c.getDateExpirationPermis());
    if (c.getDisponible() != null) {
        dto.setDisponible(c.getDisponible().name().toLowerCase());
    }
    if (c.getParc() != null) {
        dto.setId_parc(c.getParc().getId());
        dto.setParcNom(c.getParc().getNom());
    }
}

// Pour ChefDeParc
else if (user instanceof ChefDeParc) {
    ChefDeParc chef = (ChefDeParc) user;
    dto.setDateEmbauche(chef.getDateEmbauche());
    dto.setZoneAffectation(chef.getZoneAffectee());
    if (chef.getParc() != null) {
        dto.setId_parc(chef.getParc().getId());
        dto.setParcNom(chef.getParc().getNom());
    }
}

// Pour OperateurMaintenance
else if (user instanceof OperateurMaintenance) {
    OperateurMaintenance op = (OperateurMaintenance) user;
    dto.setSpecialite(op.getSpecialite());
    dto.setDateEmbauche(op.getDateEmbauche());
    // Niveau non géré dans l'entité, à voir
    if (op.getGarage() != null) {
        dto.setId_garage(op.getGarage().getId());
        dto.setGarageNom(op.getGarage().getAdresse());
    }
}
```

### B. UtilisateurService.java - update() corrections:

```java
@Transactional
public UtilisateurDTO update(Long id, UtilisateurDTO dto) {
    // 1. Récupérer l'utilisateur existant
    Utilisateur user = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // 2. Sauvegarder l'ancien rôle pour vérifier un éventuel changement
    Role ancienRole = user.getRole();
    
    // 3. Mettre à jour les champs de base
    dto.setEmail(normalizeEmail(dto.getEmail()));
    mapper.updateEntity(user, dto);

    // 4. Mettre à jour le mot de passe si fourni
    if (dto.getMotDePasse() != null && !dto.getMotDePasse().isEmpty()) {
        user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
    }
    
    // 5. Sauvegarder les modifications de base
    repository.save(user);
    
    // 6. Synchroniser les tables rôle-spécifiques
    synchroniserRoleSpecifique(user.getId(), dto, ancienRole, dto.getRole());

    return mapper.toDTO(repository.save(user));
}

private void synchroniserRoleSpecifique(Long userId, UtilisateurDTO dto, Role ancienRole, Role nouveauRole) {
    // Si le rôle a changé, supprimer l'ancien enregistrement rôle-spécifique
    if (ancienRole != nouveauRole) {
        switch (ancienRole) {
            case CHAUFFEUR:
                chauffeurRepository.deleteById(userId);
                break;
            case CHEF:
                chefDeParcRepository.deleteById(userId);
                break;
            case OPERATEUR_MAINTENANCE:
                operateurMaintenanceRepository.deleteById(userId);
                break;
        }
    }
    
    // Créer/mettre à jour l'enregistrement rôle-spécifique selon le nouveau rôle
    switch (nouveauRole) {
        case CHAUFFEUR:
            mettreAJourChauffeur(userId, dto);
            break;
        case CHEF:
            mettreAJourChefDeParc(userId, dto);
            break;
        case OPERATEUR_MAINTENANCE:
            mettreAJourOperateurMaintenance(userId, dto);
            break;
    }
}

private void mettreAJourChauffeur(Long userId, UtilisateurDTO dto) {
    Chauffeur chauffeur = chauffeurRepository.findById(userId).orElse(new Chauffeur());
    chauffeur.setId(userId);
    chauffeur.setNumeroPermis(dto.getNumeroPermis());
    chauffeur.setDateExpirationPermis(dto.getDateExpirationPermis());
    
    // Convertir String -> Disponibilite enum
    if (dto.getDisponible() != null) {
        switch (dto.getDisponible().toLowerCase()) {
            case "disponible":
                chauffeur.setDisponible(Disponibilite.DISPONIBLE);
                break;
            case "en_mission":
            case "occupe":
                chauffeur.setDisponible(Disponibilite.EN_MISSION);
                break;
            case "conge":
                chauffeur.setDisponible(Disponibilite.CONGE);
                break;
            case "malade":
                chauffeur.setDisponible(Disponibilite.MALADE);
                break;
            default:
                chauffeur.setDisponible(Disponibilite.EN_MISSION);
        }
    }
    
    if (dto.getId_parc() != null) {
        Parc parc = parcRepository.findById(dto.getId_parc()).orElse(null);
        chauffeur.setParc(parc);
    }
    
    chauffeurRepository.save(chauffeur);
}

// ... même logique pour ChefDeParc et OperateurMaintenance
```

### C. UtilisateurDTO.java - Ajouter champs:

```java
// Ajouter ces champs après les champs existants
private String parcNom;
private String garageNom;
```

## Ordre de priorité:

1. **Priorité 1**: Ajouter `parcNom` à UtilisateurDTO.java et UtilisateurMapper.java (pour l'affichage)
2. **Priorité 2**: Corriger update() dans UtilisateurService.java (pour la synchronization)
3. **Priorité 3**: Tester end-to-end

## Notes supplémentaires:

- Le champ `niveau` dans OperateurMaintenance n'existe pas dans l'entité (à vérifier si nécessaire)
- La gestion du changement de rôle doit être faite avec précaution (supprimer l'ancien enregistrement rôle-spécifique avant d'en créer un nouveau)
- Les tests doivent vérifier que la mise à jour d'un utilisateur met bien à jour toutes les tables concerné
