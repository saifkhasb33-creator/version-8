import React, { useState, useEffect, useCallback } from 'react';
import { getMaintenances, updateMaintenance, soumettreRapportMaintenance } from '../../services/maintenance';
import { useNotification } from '../../context/NotificationContext';
import '../../styles/chef.css';

function OperateurMaintenanceList({ user }) {
  const { showSuccess, showError } = useNotification();
  const [maintenances, setMaintenances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatut, setFilterStatut] = useState('');
  const [filterUrgence, setFilterUrgence] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({});
  const [sendingReportId, setSendingReportId] = useState(null);
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportText, setReportText] = useState('');
  const [selectedMaintenanceId, setSelectedMaintenanceId] = useState(null);

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

  const getFiltered = () => {
    return maintenances.filter(m => {
      if (filterStatut && m.statut !== filterStatut) return false;
      if (filterUrgence) {
        if (filterUrgence === 'URGENT' && m.cout <= 500) return false;
        if (filterUrgence === 'NORMAL' && m.cout > 500) return false;
      }
      return true;
    });
  };

  const handleUpdateStatut = async (id, newStatut) => {
    try {
      const maintenance = maintenances.find(m => m.id === id);
      await updateMaintenance(id, { ...maintenance, statut: newStatut });
      await loadMaintenances();

      const messages = {
        'EN_COURS': '✅ Maintenance marquée comme EN COURS',
        'TERMINEE': '✅ Maintenance marquée comme TERMINÉE'
      };
      showSuccess(messages[newStatut] || 'Maintenance mise à jour');
    } catch (err) {
      console.error('Erreur mise à jour maintenance', err);
      showError('Erreur lors de la mise à jour');
    }
  };

  const handleEditChange = (field, value) => {
    setEditData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSaveEdit = async (id) => {
    try {
      const maintenance = maintenances.find(m => m.id === id);
      await updateMaintenance(id, { ...maintenance, ...editData });
      setEditingId(null);
      setEditData({});
      await loadMaintenances();
      showSuccess('✅ Maintenance mise à jour');
    } catch (err) {
      console.error('Erreur sauvegarde maintenance', err);
      showError('Erreur lors de la sauvegarde');
    }
  };

  const handleOpenReportModal = (maintenanceId) => {
    setSelectedMaintenanceId(maintenanceId);
    setReportText('');
    setShowReportModal(true);
  };

  const handleCloseReportModal = () => {
    setShowReportModal(false);
    setReportText('');
    setSelectedMaintenanceId(null);
  };

  const handleSubmitRapport = async () => {
    if (!reportText.trim()) {
      showError('Veuillez rédiger un rapport avant d\'envoyer');
      return;
    }
    try {
      setSendingReportId(selectedMaintenanceId);
      console.log('📤 Soumission rapport:', { maintenanceId: selectedMaintenanceId });

      const result = await soumettreRapportMaintenance(
        selectedMaintenanceId,
        reportText
      );

      console.log('✅ Rapport soumis:', result);
      showSuccess('📄 Rapport soumis au chef de parc avec succès');
      handleCloseReportModal();
      await loadMaintenances();
    } catch (err) {
      const backendMsg = err.response?.data?.error || err.message;
      console.error('❌ Erreur soumission rapport:', backendMsg, err);
      showError(backendMsg || 'Erreur lors de la soumission du rapport');
    } finally {
      setSendingReportId(null);
    }
  };

  const getStatutLabel = (statut) => {
    const labels = {
      'PLANIFIEE': { text: '📅 Planifiée', class: 'warning' },
      'EN_COURS': { text: '⚙️ En cours', class: 'active' },
      'TERMINEE': { text: '✅ Terminée', class: 'success' },
    };
    return labels[statut] || { text: statut, class: '' };
  };

  const getUrgence = (cout) => {
    if (cout > 1000) return { text: '🔴 URGENT', class: 'danger' };
    if (cout > 500) return { text: '🟠 IMPORTANT', class: 'warning' };
    return { text: '🟢 Normal', class: 'success' };
  };

  const filtered = getFiltered();

  if (loading) return <div className="loading">Chargement...</div>;

  return (
    <div className="chef-container">
      <div className="section-header">
        <h1>🔧 Maintenances Assignées</h1>
        <p>Gérez les maintenances assignées par le chef de parc</p>
      </div>

      {/* Filtres */}
      <div className="filters-container">
        <div className="filter-group">
          <label>Statut:</label>
          <select value={filterStatut} onChange={(e) => setFilterStatut(e.target.value)}>
            <option value="">Tous</option>
            <option value="PLANIFIEE">📅 Planifiée</option>
            <option value="EN_COURS">⚙️ En cours</option>
            <option value="TERMINEE">✅ Terminée</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Urgence:</label>
          <select value={filterUrgence} onChange={(e) => setFilterUrgence(e.target.value)}>
            <option value="">Tous</option>
            <option value="URGENT">🔴 Urgent</option>
            <option value="NORMAL">🟢 Normal</option>
          </select>
        </div>
      </div>

      {/* Tableau */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Véhicule</th>
              <th>Type Maintenance</th>
              <th>Date Prévue</th>
              <th>Urgence</th>
              <th>Statut</th>
              <th>Coût</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length > 0 ? (
              filtered.map((m) => {
                const isEditing = editingId === m.id;
                const urgence = getUrgence(m.cout);
                const statut = getStatutLabel(m.statut);

                return (
                  <tr key={m.id} className={isEditing ? 'editing' : ''}>
                    <td><strong>{m.vehicule?.immatriculation || 'N/A'}</strong></td>
                    <td>
                      {isEditing ? (
                        <input
                          type="text"
                          value={editData.type || m.type}
                          onChange={(e) => handleEditChange('type', e.target.value)}
                          className="input-field"
                        />
                      ) : (
                        m.type
                      )}
                    </td>
                    <td>{new Date(m.datePrevue).toLocaleDateString('fr-FR')}</td>
                    <td>
                      <span className={`badge badge-${urgence.class}`}>
                        {urgence.text}
                      </span>
                    </td>
                    <td>
                      <span className={`badge badge-${statut.class}`}>
                        {statut.text}
                      </span>
                    </td>
                    <td>
                      {isEditing ? (
                        <input
                          type="number"
                          value={editData.cout || m.cout}
                          onChange={(e) => handleEditChange('cout', parseFloat(e.target.value))}
                          className="input-field"
                        />
                      ) : (
                        `${m.cout || 0} TND`
                      )}
                    </td>
                    <td className="actions-cell">
                      {!isEditing ? (
                        <>
                          <button
                            className="btn btn-sm btn-primary"
                            onClick={() => {
                              setEditingId(m.id);
                              setEditData({
                                type: m.type,
                                cout: m.cout,
                                description: m.description
                              });
                            }}
                          >
                            ✏️ Modifier
                          </button>

                          {m.statut !== 'EN_COURS' && (
                            <button
                              className="btn btn-sm btn-warning"
                              onClick={() => handleUpdateStatut(m.id, 'EN_COURS')}
                            >
                              ▶️ Commencer
                            </button>
                          )}

                          {m.statut !== 'TERMINEE' && (
                            <button
                              className="btn btn-sm btn-success"
                              onClick={() => handleUpdateStatut(m.id, 'TERMINEE')}
                            >
                              ✓ Terminer
                            </button>
                          )}

                          {(m.statut === 'EN_COURS' || m.statut === 'TERMINEE') && (
                            <button
                              className="btn btn-sm btn-info"
                              onClick={() => handleOpenReportModal(m.id)}
                              disabled={sendingReportId === m.id}
                            >
                              {sendingReportId === m.id ? '⏳ Envoi...' : '📝 Rédiger rapport'}
                            </button>
                          )}
                        </>
                      ) : (
                        <>
                          <button
                            className="btn btn-sm btn-success"
                            onClick={() => handleSaveEdit(m.id)}
                          >
                            💾 Sauvegarder
                          </button>
                          <button
                            className="btn btn-sm btn-secondary"
                            onClick={() => {
                              setEditingId(null);
                              setEditData({});
                            }}
                          >
                            ❌ Annuler
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="7" className="text-center">Aucune maintenance</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Résumé */}
      <div className="summary-box">
        <h3>📊 Résumé</h3>
        <div className="summary-stats">
          <div className="stat">
            <strong>{filtered.length}</strong>
            <span>Total maintenances</span>
          </div>
          <div className="stat">
            <strong>{filtered.filter(m => m.statut === 'PLANIFIEE').length}</strong>
            <span>Planifiées</span>
          </div>
          <div className="stat">
            <strong>{filtered.filter(m => m.statut === 'EN_COURS').length}</strong>
            <span>En cours</span>
          </div>
          <div className="stat">
            <strong>{filtered.filter(m => m.statut === 'TERMINEE').length}</strong>
            <span>Terminées</span>
          </div>
        </div>
      </div>

      {/* Modal de rédaction du rapport */}
      {showReportModal && (
        <div className="modal-overlay" onClick={handleCloseReportModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>📝 Rédiger le rapport de maintenance</h3>
              <button className="modal-close" onClick={handleCloseReportModal}>✕</button>
            </div>
            <div className="modal-body">
              <label>Description des travaux réalisés, observations, pièces remplacées, etc. :</label>
              <textarea
                value={reportText}
                onChange={(e) => setReportText(e.target.value)}
                placeholder="Rédigez votre rapport détaillé ici..."
                rows="10"
                className="report-textarea"
              />
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={handleCloseReportModal}>
                Annuler
              </button>
              <button
                className="btn btn-primary"
                onClick={handleSubmitRapport}
                disabled={sendingReportId === selectedMaintenanceId || !reportText.trim()}
              >
                {sendingReportId === selectedMaintenanceId ? '⏳ Envoi...' : '📤 Soumettre le rapport'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default OperateurMaintenanceList;

