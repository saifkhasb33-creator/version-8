package com.parc.service;

import com.parc.domain.entity.Utilisateur;
import com.parc.domain.entity.Chauffeur;
import com.parc.domain.entity.ChefDeParc;
import com.parc.domain.entity.OperateurMaintenance;
import com.parc.domain.entity.Parc;
import com.parc.domain.enums.Disponibilite;
import com.parc.domain.enums.Role;
import com.parc.dto.UtilisateurDTO;
import com.parc.mapper.UtilisateurMapper;
import com.parc.repository.ChauffeurRepository;
import com.parc.repository.ChefDeParcRepository;
import com.parc.repository.NotificationRepository;
import com.parc.repository.OperateurMaintenanceRepository;
import com.parc.repository.UtilisateurRepository;
import com.parc.repository.ParcRepository;
import com.parc.repository.GarageRepository;
import com.parc.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository repository;
    private final UtilisateurMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final ChauffeurRepository chauffeurRepository;
    private final ChefDeParcRepository chefDeParcRepository;
    private final OperateurMaintenanceRepository operateurMaintenanceRepository;
private final NotificationRepository notificationRepository;
    private final ParcRepository parcRepository;
    private final GarageRepository garageRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = normalizeEmail(email);

        Utilisateur user = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + normalizedEmail));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getMotDePasse(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.isActif()
        );
    }

    @Transactional
    public boolean authenticateLegacyPasswordAndUpgrade(String email, String rawPassword) {
        if (rawPassword == null) {
            return false;
        }

        Utilisateur user = repository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
        if (user == null || user.getMotDePasse() == null || !user.isActif()) {
            return false;
        }

        String storedPassword = user.getMotDePasse();

        // Compatibilite legacy: ancien mot de passe stocke en clair.
        if (!isBcryptHash(storedPassword) && storedPassword.equals(rawPassword)) {
            user.setMotDePasse(passwordEncoder.encode(rawPassword));
            repository.save(user);
            return true;
        }

        return false;
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }

@Transactional
    public UtilisateurDTO create(UtilisateurDTO dto) {
        dto.setEmail(normalizeEmail(dto.getEmail()));

        String rawPassword = dto.getMotDePasse();

        if (rawPassword == null || rawPassword.isEmpty()) {
            rawPassword = "default123";
        }

        // Toujours stocker un mot de passe chiffre (BCrypt).
        dto.setMotDePasse(passwordEncoder.encode(rawPassword));

        // Sauvegarder d'abord l'utilisateur dans la table utilisateurs
        Utilisateur user = mapper.toEntity(dto);
        Utilisateur savedUser = repository.save(user);
        
        // Creer l'entree role-specifique si necessaire
        Long userId = savedUser.getId();
        
        if ("CHAUFFEUR".equals(dto.getRole())) {
            // Creer dans la table chauffeurs
            Chauffeur chauffeur = new Chauffeur();
            chauffeur.setId(userId);
            chauffeur.setNom(savedUser.getNom());
            chauffeur.setPrenom(savedUser.getPrenom());
            chauffeur.setEmail(savedUser.getEmail());
            chauffeur.setTelephone(savedUser.getTelephone());
            chauffeur.setNumeroPermis(dto.getNumeroPermis());
            chauffeur.setDateExpirationPermis(dto.getDateExpirationPermis());
            
// Convertir String -> Disponibilite enum
            if ("disponible".equals(dto.getDisponible())) {
                chauffeur.setDisponible(Disponibilite.DISPONIBLE);
            } else if ("en_mission".equals(dto.getDisponible())) {
                chauffeur.setDisponible(Disponibilite.EN_MISSION);
            } else if ("conge".equals(dto.getDisponible())) {
                chauffeur.setDisponible(Disponibilite.CONGE);
            } else if ("malade".equals(dto.getDisponible())) {
                chauffeur.setDisponible(Disponibilite.MALADE);
            } else {
                chauffeur.setDisponible(Disponibilite.EN_MISSION);
            }
            
            if (dto.getId_parc() != null) {
                Parc parc = parcRepository.findById(dto.getId_parc()).orElse(null);
                chauffeur.setParc(parc);
            }
            chauffeurRepository.save(chauffeur);
        } 
        else if ("CHEF".equals(dto.getRole())) {
            // Creer dans la table chef_de_parc
            ChefDeParc chef = new ChefDeParc();
            chef.setId(userId);
            chef.setNom(savedUser.getNom());
            chef.setPrenom(savedUser.getPrenom());
            chef.setEmail(savedUser.getEmail());
            chef.setTelephone(savedUser.getTelephone());
            chef.setDateEmbauche(dto.getDateEmbauche());
            chef.setZoneAffectee(dto.getZoneAffectation());
            
            if (dto.getId_parc() != null) {
                Parc parc = parcRepository.findById(dto.getId_parc()).orElse(null);
                chef.setParc(parc);
            }
            chefDeParcRepository.save(chef);
        }
        else if ("OPERATEUR_MAINTENANCE".equals(dto.getRole())) {
            // Creer dans la table operateur_maintenance
            OperateurMaintenance operateur = new OperateurMaintenance();
            operateur.setId(userId);
            operateur.setNom(savedUser.getNom());
            operateur.setPrenom(savedUser.getPrenom());
            operateur.setEmail(savedUser.getEmail());
            operateur.setTelephone(savedUser.getTelephone());
            operateur.setSpecialite(dto.getSpecialite());
            operateur.setDateEmbauche(dto.getDateEmbauche());
            
            if (dto.getId_garage() != null) {
                operateur.setGarage(garageRepository.findById(dto.getId_garage()).orElse(null));
            }
            operateurMaintenanceRepository.save(operateur);
        }

        return mapper.toDTO(savedUser);
    }

    public List<UtilisateurDTO> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public UtilisateurDTO getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

@Transactional
    public UtilisateurDTO update(Long id, UtilisateurDTO dto) {

        Utilisateur user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Sauvegarder l'ancien rôle pour vérifier un éventuel changement
        Role ancienRole = user.getRole();

        // Mettre à jour l'entité (sauf le mot de passe)
        dto.setEmail(normalizeEmail(dto.getEmail()));
        mapper.updateEntity(user, dto);

        // Mettre à jour le mot de passe UNIQUEMENT s'il est fourni
        if (dto.getMotDePasse() != null && !dto.getMotDePasse().isEmpty()) {
            // Hashage sécurisé du mot de passe
            user.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }

        // Sauvegarder les modifications de base
        Utilisateur savedUser = repository.save(user);

// Synchroniser les tables rôle-spécifiques
        synchroniserRoleSpecifique(id, dto, ancienRole, dto.getRole());

        return mapper.toDTO(savedUser);
    }

private void synchroniserRoleSpecifique(Long userId, UtilisateurDTO dto, Role ancienRole, Role nouveauRole) {
        if (nouveauRole == null) return;

        // Si le rôle a changé, supprimer l'ancien enregistrement rôle-spécifique
        if (ancienRole != nouveauRole) {
            switch (ancienRole) {
                case CHAUFFEUR:
                    if (chauffeurRepository.existsById(userId)) {
                        chauffeurRepository.deleteById(userId);
                    }
                    break;
                case CHEF:
                    if (chefDeParcRepository.existsById(userId)) {
                        chefDeParcRepository.deleteById(userId);
                    }
                    break;
                case OPERATEUR_MAINTENANCE:
                    if (operateurMaintenanceRepository.existsById(userId)) {
                        operateurMaintenanceRepository.deleteById(userId);
                    }
                    break;
                default:
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
            default:
                break;
        }
    }

    private void mettreAJourChauffeur(Long userId, UtilisateurDTO dto) {
        // First update the base Utilisateur fields in chauffeur table
        Chauffeur chauffeur = chauffeurRepository.findById(userId).orElse(null);

        if (chauffeur == null) {
            // Create new if doesn't exist
            chauffeur = new Chauffeur();
            chauffeur.setId(userId);
        }

        // Update common fields
        Utilisateur user = repository.findById(userId).orElse(null);
        if (user != null) {
            chauffeur.setNom(user.getNom());
            chauffeur.setPrenom(user.getPrenom());
            chauffeur.setEmail(user.getEmail());
            chauffeur.setTelephone(user.getTelephone());
        }

        // Update chauffeur-specific fields
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

    private void mettreAJourChefDeParc(Long userId, UtilisateurDTO dto) {
        ChefDeParc chef = chefDeParcRepository.findById(userId).orElse(null);

        if (chef == null) {
            chef = new ChefDeParc();
            chef.setId(userId);
        }

        // Update common fields
        Utilisateur user = repository.findById(userId).orElse(null);
        if (user != null) {
            chef.setNom(user.getNom());
            chef.setPrenom(user.getPrenom());
            chef.setEmail(user.getEmail());
            chef.setTelephone(user.getTelephone());
        }

        // Update chef-specific fields
        chef.setDateEmbauche(dto.getDateEmbauche());
        chef.setZoneAffectee(dto.getZoneAffectation());

        if (dto.getId_parc() != null) {
            Parc parc = parcRepository.findById(dto.getId_parc()).orElse(null);
            chef.setParc(parc);
        }

        chefDeParcRepository.save(chef);
    }

    private void mettreAJourOperateurMaintenance(Long userId, UtilisateurDTO dto) {
        OperateurMaintenance operateur = operateurMaintenanceRepository.findById(userId).orElse(null);

        if (operateur == null) {
            operateur = new OperateurMaintenance();
            operateur.setId(userId);
        }

        // Update common fields
        Utilisateur user = repository.findById(userId).orElse(null);
        if (user != null) {
            operateur.setNom(user.getNom());
            operateur.setPrenom(user.getPrenom());
            operateur.setEmail(user.getEmail());
            operateur.setTelephone(user.getTelephone());
        }

        // Update operateur-specific fields
        operateur.setSpecialite(dto.getSpecialite());
        operateur.setDateEmbauche(dto.getDateEmbauche());

        if (dto.getId_garage() != null) {
            operateur.setGarage(garageRepository.findById(dto.getId_garage()).orElse(null));
        }

        operateurMaintenanceRepository.save(operateur);
    }

    @Transactional
    public void delete(Long id) {
        try {
            // Delete notifications for this utilisateur (references destinataire_id)
            notificationRepository.deleteByDestinataireId(id);

            // Delete child records if they exist
            if (chauffeurRepository.existsById(id)) {
                chauffeurRepository.deleteById(id);
            }
            if (chefDeParcRepository.existsById(id)) {
                chefDeParcRepository.deleteById(id);
            }
            if (operateurMaintenanceRepository.existsById(id)) {
                operateurMaintenanceRepository.deleteById(id);
            }

            // Now delete the parent utilisateur
            repository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    public UtilisateurDTO getByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return repository.findByEmailIgnoreCase(normalizedEmail)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec email : " + normalizedEmail));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}