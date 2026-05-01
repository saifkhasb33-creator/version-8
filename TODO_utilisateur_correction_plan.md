# Plan de Correction - Gestion Utilisateurs

## Résumé de l'Analyse

### Fichiers examinés:
1. **Backend:**
   - `UtilisateurService.java` - ✅ Logique de sync role-spécifique déjà en place
   - `UtilisateurMapper.java` - ✅ Mapping des champs rôle-spécifiques correct
   - `UtilisateurDTO.java` - ✅ Champs `parcNom` et `garageNom` présents
   - `UtilisateurController.java` - ✅ Endpoints REST corrects

2. **Frontend:**
   - `UserForm.js` - ⚠️ Problèmes d'alignement des valeurs
   - `UserList.js` - ✅ Affichage correct

### Problèmes identifiés:

#### 1. UserForm.js - Disponibilité mismatch
Les valeurs dans le select ne correspondent pas à ce que le backend attend:
```javascript
// Frontend (actuel - incorrect)
<option value="occupé">Occupé</option>
<option value="congé">En congé</option>

// Backend attend:
"en_mission" (pour EN_ISSION)
"conge" (pour CONGE)
```

#### 2. UserForm.js - Dates mal formatées
Les champs date sont envoyée comme string sans transformation
- `dateExpirationPermis` - input type="date" OK
- `dateEmbauche` - input type="date" OK

#### 3. Backend - Garantir la sync complète
S'assurer que `update()` sync bien les champs role-spécifiques

---

## Corrections à appliquer

### A. UserForm.js - Corriger les valeurs de disponibilité

#### Problème: Le select utilise des valeurs françaises qui ne matchent pas le backend

```javascript
// AVANT (incorrect)
<select name="disponible" value={user.disponible} onChange={handleChange}>
  <option value="disponible">Disponible</option>
  <option value="occupe">Occupé</option>
  <option value="congé">En congé</option>
</select>

// APRÈS (correct)
<select name="disponible" value={user.disponible} onChange={handleChange}>
  <option value="disponible">Disponible</option>
  <option value="en_mission">En mission</option>
  <option value="conge">En congé</option>
  <option value="malade">Malade</option>
</select>
```

### B. Backend - Améliorer la robustesse de UtilisateurService

#### Garantir que update() sync tous les champs

Le code actuel semble correct, mais on peut ajouter:
1. Des logs pour debugger
2. Des vérifications null safety
3. Gestion du cas où les champs optionnels ne sont pas fournis

---

## Ordre de priorité

1. **Priorité 1 (HAUTE):** Corriger UserForm.js - Disponibilité
2. **Priorité 2 (MOYENNE):** Vérifier UtilisateurService.update() - Sync complète
3. **Priorité 3 (FAIBLE):** Ajouter logging pour debugging

---

## Détails des corrections

### Step 1: Corriger UserForm.js - Disponibilité

Fichier: `frontend/src/components/admin/UserForm.js`

Section à modifier (ligne ~145):
```javascript
{/* Champs spécifiques au rôle */}
// ... code existant ...

<div className="form-group">
  <label>Disponibilité</label>
  <select name="disponible" value={user.disponible} onChange={handleChange}>
    <option value="disponible">Disponible</option>
    <option value="en_mission">En mission</option>
    <option value="conge">En congé</option>
    <option value="malade">Malade</option>
  </select>
</div>
```

### Step 2: Corriger UserForm.js - Valeur par défaut date

Le champ dateExpirationPermis doit avoir une valeur par défaut vide:
```javascript
const [user, setUser] = useState({
  // ...
  dateExpirationPermis: '',
  // ...
});
```

### Step 3: Vérifier UtilisateurService.update()

S'assurer que la méthode `synchroniserRoleSpecifique` est appelée avec les bons paramètres.

---

## Tests à effectuer

1. **Création utilisateur chauffeur:**
   - Créer un nouveau chauffeur avec tous les champs
   - Vérifier dans la table `chauffeurs`
   
2. **Modification utilisateur:**
   - Modifier un chauffeur existant
   - Changer disponibilité → Vérifier sync
   - Changer parc → Vérifier sync

3. **Changement de rôle:**
   - Changer un chauffeur → Chef
   - Vérifier suppression dans `chauffeurs`
   - Vérifier création dans `chef_de_parc`

---

## Notes

- Le champ `disponible` est un enum `Disponibilite` avec valeurs:
  - DISPONIBLE
  - EN_ISSION
  - CONGE
  - MALADE

- Dans le frontend, on utilise des strings en minuscules:
  - "disponible" → DISPONIBLE
  - "en_ission" → EN_ISSION
  - "conge" → CONGE
  - "malade" → MALADE
