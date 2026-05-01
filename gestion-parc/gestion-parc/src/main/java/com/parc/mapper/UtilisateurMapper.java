package com.parc.mapper;

import com.parc.domain.entity.Utilisateur;
import com.parc.dto.UtilisateurDTO;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

public UtilisateurDTO toDTO(Utilisateur user) {
        if (user == null) return null;
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(user.getId());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setEmail(user.getEmail());
        dto.setTelephone(user.getTelephone());
        dto.setRole(user.getRole());
        dto.setActif(user.isActif());
        dto.setPhoto(user.getPhoto());
        // ⚠️ NE JAMAIS exposer le mot de passe au frontend
        // dto.setMotDePasse(user.getMotDePasse());

        // Mapper les champs rôle-spécifiques
        if (user instanceof com.parc.domain.entity.Chauffeur) {
            com.parc.domain.entity.Chauffeur c = (com.parc.domain.entity.Chauffeur) user;
            dto.setNumeroPermis(c.getNumeroPermis());
            dto.setDateExpirationPermis(c.getDateExpirationPermis());
            if (c.getDisponible() != null) {
                dto.setDisponible(c.getDisponible().name().toLowerCase());
            }
            if (c.getParc() != null) {
                dto.setId_parc(c.getParc().getId());
                dto.setParcNom(c.getParc().getNom());
            }
        } else if (user instanceof com.parc.domain.entity.ChefDeParc) {
            com.parc.domain.entity.ChefDeParc chef = (com.parc.domain.entity.ChefDeParc) user;
            dto.setDateEmbauche(chef.getDateEmbauche());
            dto.setZoneAffectation(chef.getZoneAffectee());
            if (chef.getParc() != null) {
                dto.setId_parc(chef.getParc().getId());
                dto.setParcNom(chef.getParc().getNom());
            }
        } else if (user instanceof com.parc.domain.entity.OperateurMaintenance) {
            com.parc.domain.entity.OperateurMaintenance op = (com.parc.domain.entity.OperateurMaintenance) user;
            dto.setSpecialite(op.getSpecialite());
            dto.setDateEmbauche(op.getDateEmbauche());
            if (op.getGarage() != null) {
                dto.setId_garage(op.getGarage().getId());
                dto.setGarageNom(op.getGarage().getAdresse());
            }
        }

        return dto;
    }

public Utilisateur toEntity(UtilisateurDTO dto) {
        if (dto == null) return null;
        Utilisateur user = new Utilisateur();
        user.setId(dto.getId());
        user.setNom(dto.getNom());
        user.setPrenom(dto.getPrenom());
        user.setEmail(dto.getEmail());
        user.setTelephone(dto.getTelephone());
        user.setRole(dto.getRole());
        user.setActif(dto.isActif());
        user.setPhoto(dto.getPhoto());
        user.setMotDePasse(dto.getMotDePasse());
        return user;
    }

    public void updateEntity(Utilisateur user, UtilisateurDTO dto) {
        if (user == null || dto == null) return;
        user.setNom(dto.getNom());
        user.setPrenom(dto.getPrenom());
        user.setEmail(dto.getEmail());
        user.setTelephone(dto.getTelephone());
        user.setRole(dto.getRole());
        user.setActif(dto.isActif());
        user.setPhoto(dto.getPhoto());
        // ⚠️ NE PAS mettre à jour le motDePasse ici
        // Le mot de passe doit être géré uniquement par UtilisateurService
        // qui gère le hachage BCrypt
    }
}