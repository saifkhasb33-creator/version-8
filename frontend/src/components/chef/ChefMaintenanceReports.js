import React, { useState, useEffect, useCallback } from 'react';
import { getMaintenances, telechargerRapportPdf } from '../../services/maintenance';
import { useNotification } from '../../context/NotificationContext';
import '../../styles/chef.css';

function ChefMaintenanceReports({ user }) {
  const { showSuccess, showError } = useNotification();
  const [maintenances, setMaintenances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatut, setFilterStatut] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);

  const loadMaintenances = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getMaintenances();
      setMaintenances(response.data || []);
    } catch (err) {
      console.error('Erreur chargement maintenances', err);
      showError('Erreur lors du chargement des maintenances');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    loadMaintenances();
    const interval = setInterval(loadMaintenances, 30000);
    return () => clearInterval(interval);
  }, [loadMaintenances]);

  const handleDownloadPdf = async (maintenanceId) => {
    try {
      setDownloadingId(maintenanceId);
      await telechargerRapportPdf(maintenanceId);
      showSuccess('✅ Téléchargement du rapport PDF démarré');
    } catch (err) {
      console.error('Erreur téléchargement PDF', err);
      showError('❌ Erreur lors du téléchargement du rapport');
    } finally {
      setDownloadingId(null);
    }
  };

  const getStatutLabel = (statut) => {
    const labels = {
      'PLANIFIEE': { text: '📅 Planifiée', class: 'warning' },
      'EN_COURS': { text: '🔧 En cours', class: 'active' },
      'TERMINEE': { text: '✅ Terminée', class: 'success' },
      'ANNULEE': { text: '❌ Annulée', class: 'danger' }
    };
    return labels[statut] || { text: statut, class: '' };
  };

  const filtered = maintenances.filter(m => {
    if (filterStatut && m.statut !== filterStatut) return false;
    return true;
  });

  if (loading) return <div className="loading">Chargement...</div>;

  return (
    <div className="chef-container">
      <div className="section-header">
        <h1>📄 Rapports de Maintenance</h1>
        <p>Consultez et téléchargez les rapports de maintenance PDF envoyés par les opérateurs</p>
      </div>

      <div className="filters-bar">
        <div className="filter-select">
          <label>Statut :</label>
          <select value={filterStatut} onChange={(e) => setFilterStatut(e.target.value)}>
            <option value="">Tous les statuts</option>
            <option value="PLANIFIEE">Planifiée</option>
            <option value="EN_COURS">En cours</option>
            <option value="TERMINEE">Terminée</option>
            <option value="ANNULEE">Annulée</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        {filtered.length === 0 ? (
          <div className="empty-state">Aucune maintenance trouvée</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Véhicule</th>
                <th>Garage</th>
                <th>Date prévue</th>
                <th>Date réalisation</th>
                <th>Statut</th>
                <th>Coût</th>
                <th>Rapport PDF</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(m => {
                const s = getStatutLabel(m.statut);
                return (
                  <tr key={m.id}>
                    <td>{m.id}</td>
                    <td><strong>{m.type}</strong></td>
                    <td>{m.vehiculeMatricule || 'N/A'}</td>
                    <td>{m.garageNom || '-'}</td>
                    <td>{m.datePrevue}</td>
                    <td>{m.dateRealisation || '-'}</td>
                    <td><span className={`status-badge ${s.class}`}>{s.text}</span></td>
                    <td>{m.cout ? `${m.cout} TND` : '-'}</td>
                    <td>
                      <button
                        className="btn btn-sm btn-primary"
                        onClick={() => handleDownloadPdf(m.id)}
                        disabled={downloadingId === m.id}
                      >
                        {downloadingId === m.id ? '⏳ Téléchargement...' : '📥 Télécharger PDF'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default ChefMaintenanceReports;

