import api from './api';

// ─── CRUD de base ────────────────────────────────────────
export const getUtilisateurs   = ()        => api.get('/utilisateurs');
export const getUtilisateur    = (id)      => api.get(`/utilisateurs/${id}`);
export const createUtilisateur = (data)    => api.post('/utilisateurs', data);
export const updateUtilisateur = (id, data)=> api.put(`/utilisateurs/${id}`, data);
export const deleteUtilisateur = (id)      => api.delete(`/utilisateurs/${id}`);

// ─── Photo ───────────────────────────────────────────────

/**
 * Upload d'une photo via multipart/form-data.
 * @param {number} id      - ID de l'utilisateur
 * @param {File}   file    - Fichier image sélectionné
 */
export const uploadPhoto = (id, file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post(`/utilisateurs/${id}/photo`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

/**
 * Récupère uniquement la photo (+ nom/prénom) d'un utilisateur.
 */
export const getPhoto = (id) => api.get(`/utilisateurs/${id}/photo`);

/**
 * Supprime la photo d'un utilisateur.
 */
export const deletePhoto = (id) => api.delete(`/utilisateurs/${id}/photo`);

// ─── Utilisateur connecté ────────────────────────────────
export const getCurrentUser = () => api.get('/utilisateurs/me');

// ─── Réinitialisation mot de passe ───────────────────────
export const resetPassword        = (id, newPassword)   =>
  api.post(`/utilisateurs/${id}/reset-password`, { newPassword });

export const resetPasswordByEmail = (email, newPassword) =>
  api.post(`/utilisateurs/reset/${email}`, { newPassword });