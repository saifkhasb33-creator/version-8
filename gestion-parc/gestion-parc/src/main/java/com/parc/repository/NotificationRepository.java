package com.parc.repository;

import com.parc.domain.entity.Notification;
import com.parc.domain.enums.StatutNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    void deleteByDestinataireId(Long destinataireId);

    @Query("SELECT n FROM Notification n " +
           "LEFT JOIN FETCH n.destinataire " +
           "LEFT JOIN FETCH n.mission " +
           "LEFT JOIN FETCH n.maintenance " +
           "WHERE n.destinataire.id = :destinataireId " +
           "ORDER BY n.dateEnvoi DESC")
    List<Notification> findByDestinataireIdWithRelations(@Param("destinataireId") Long destinataireId);

    List<Notification> findByDestinataireIdOrderByDateEnvoiDesc(Long destinataireId);
    List<Notification> findByDestinataireIdAndStatutOrderByDateEnvoiDesc(Long destinataireId, StatutNotification statut);
    long countByDestinataireIdAndStatut(Long destinataireId, StatutNotification statut);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.statut = :statut WHERE n.id = :id")
    void updateStatut(@Param("id") Long id, @Param("statut") StatutNotification statut);
}
