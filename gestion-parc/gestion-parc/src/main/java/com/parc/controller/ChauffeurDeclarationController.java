package com.parc.controller;

import com.parc.dto.AccidentDTO;
import com.parc.dto.AmendeDTO;
import com.parc.security.CustomUserDetails;
import com.parc.service.AccidentService;
import com.parc.service.AmendeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chauffeur/declarations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChauffeurDeclarationController {

    private final AccidentService accidentService;
    private final AmendeService amendeService;

    // Déclarer une amende
    @PostMapping("/amende")
    public ResponseEntity<AmendeDTO> declarerAmende(@RequestBody AmendeDTO dto,
                                                    @AuthenticationPrincipal CustomUserDetails user) {
        dto.setChauffeurId(user.getId());
        return ResponseEntity.ok(amendeService.create(dto));
    }

    // Liste des amendes du chauffeur connecté
    @GetMapping("/amendes")
    public ResponseEntity<List<AmendeDTO>> getMesAmendes(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(amendeService.getByChauffeur(user.getId()));
    }

    // Déclarer un accident
    @PostMapping("/accident")
    public ResponseEntity<AccidentDTO> declarerAccident(@RequestBody AccidentDTO dto,
                                                        @AuthenticationPrincipal CustomUserDetails user) {
        dto.setChauffeurId(user.getId());
        return ResponseEntity.ok(accidentService.create(dto));
    }

    // Liste des accidents du chauffeur connecté
    @GetMapping("/accidents")
    public ResponseEntity<List<AccidentDTO>> getMesAccidents(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(accidentService.getByChauffeur(user.getId()));
    }
}