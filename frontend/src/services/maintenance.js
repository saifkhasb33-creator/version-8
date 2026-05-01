import api from './api';

// Endpoints pour la gestion des maintenances
export const createMaintenance = (data) => api.post('/maintenances', data);
export const getMaintenances = () => api.get('/maintenances');
export const getMaintenanceById = (id) => api.get(`/maintenances/${id}`);
export const updateMaintenance = (id, data) => api.put(`/maintenances/${id}`, data);
export const deleteMaintenance = (id) => api.delete(`/maintenances/${id}`);
export const getMaintenancesByVehicule = (vehiculeId) => api.get(`/maintenances/vehicule/${vehiculeId}`);
export const getMaintenancesByGarage = (garageId) => api.get(`/maintenances/garage/${garageId}`);
export const envoyerRapportMaintenanceAuChef = (maintenanceId) =>
  api.post(`/operateurs-maintenance/maintenances/${maintenanceId}/envoyer-rapport-chef`);

export const soumettreRapportMaintenance = (maintenanceId, rapportText) =>
  api.post(`/operateurs-maintenance/maintenances/${maintenanceId}/soumettre-rapport`, {
    rapportText
  });

/**
 * Télécharger le rapport PDF d'une maintenance
 * @param {number} maintenanceId - ID de la maintenance
 */
export const telechargerRapportPdf = async (maintenanceId) => {
  const response = await api.get(`/operateurs-maintenance/maintenances/${maintenanceId}/rapport-pdf`, {
    responseType: 'blob'
  });
  const blob = new Blob([response.data], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `rapport-maintenance-${maintenanceId}.pdf`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
};
