package com.parc.controller;

import com.parc.dto.NotificationDTO;
import com.parc.security.CustomUserDetails;
import com.parc.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Récupérer toutes les notifications de l'utilisateur connecté
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationDTO> notifications = notificationService.getMesNotifications(userDetails.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Récupérer les notifications non lues
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationDTO> notifications = notificationService.getMesNotifications(userDetails.getId()).stream()
                .filter(n -> "NON_LUE".equals(n.getStatut()))
                .toList();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Compter les notifications non lues
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnread(@AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getNonLuesCount(userDetails.getId());
        return ResponseEntity.ok(count);
    }

    /**
     * Marquer une notification comme lue
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.marquerToutesCommeLues(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Supprimer une notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprimer toutes les notifications
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteAllNotifications(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
