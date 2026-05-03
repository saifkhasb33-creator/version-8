import api from './api';

// Récupérer les notifications du chauffeur
export const getNotifications = () => api.get('/notifications');

// Récupérer les notifications non lues
export const getUnreadNotifications = () => api.get('/notifications/unread');

// Nombre de notifications non lues (badge cloche)
export const getUnreadNotificationCount = () => api.get('/notifications/unread/count');

// Marquer une notification comme lue
export const markAsRead = (id) => api.put(`/notifications/${id}/read`);

// Marquer toutes les notifications comme lues
export const markAllAsRead = () => api.put('/notifications/read-all');

// Supprimer une notification
export const deleteNotification = (id) => api.delete(`/notifications/${id}`);

// --- Compatibilité avec l'ancien naming utilisé dans certains composants ---
export const marquerLu = markAsRead;
export const marquerTousLus = markAllAsRead;
export const supprimerNotification = deleteNotification;
