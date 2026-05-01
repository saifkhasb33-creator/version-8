import React, { useState, useEffect } from 'react';
import { getMaintenances, envoyerRapportMaintenanceAuChef, soumettreRapportMaintenance } from '../../services/maintenance';
import { useNotification } from '../../context/NotificationContext';
import '../../styles/chef.css';

function OperateurMaintenanceReports({ user }) {
  const { showError, showSuccess } = useNotification();
  const [maintenances, setMaintenances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState('month'); // month, quarter, year
  const [reportType, setReportType] = useState('summary'); // summary, detailed, by-vehicle
  const [sendingReportId, setSendingReportId] = useState(null);
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportText, setReportText] = useState('');
  const [selectedMaintenanceId, setSelectedMaintenanceId] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
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
  };

  const getFilteredData = () => {
    const now = new Date();
    const filtered = maintenances.filter(m => {
      const mDate = new Date(m.datePrevue);
      let startDate = new Date();

      if (period === 'month') {
        startDate = new Date(now.getFullYear(), now.getMonth(), 1);
      } else if (period === 'quarter') {
        const quarter = Math.floor(now.getMonth() / 3);
        startDate = new Date(now.getFullYear(), quarter * 3, 1);
      } else if (period === 'year') {
        startDate = new Date(now.getFullYear(), 0, 1);
      }

      return mDate >= startDate;
    });

    return filtered;
  };

  const handleEnvoyerRapportPdf = async (maintenanceId) => {
    try {
      await envoyerRapportMaintenanceAuChef(maintenanceId);
      showSuccess('✅ Rapport PDF envoye au chef. Une notification a ete creee.');
    } catch (err) {
      console.error('Erreur envoi rapport PDF', err);
      showError("❌ Impossible d'envoyer le rapport PDF au chef");
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
      await soumettreRapportMaintenance(selectedMaintenanceId, reportText);
      showSuccess('📄 Rapport soumis au chef de parc avec succès');
      handleCloseReportModal();
      await loadData();
    } catch (err) {
      const backendMsg = err.response?.data?.error || err.message;
      console.error('❌ Erreur soumission rapport:', backendMsg, err);
      showError(backendMsg || 'Erreur lors de la soumission du rapport');
    } finally {
      setSendingReportId(null);
    }
  };

  const generateSummaryReport = () => {
    const filtered = getFilteredData();
    const total = filtered.length;
    const completed = filtered.filter(m => m.statut === 'TERMINEE').length;
    const inProgress = filtered.filter(m => m.statut === 'EN_COURS').length;
    const planned = filtered.filter(m => m.statut === 'PLANIFIEE').length;
    const totalCost = filtered.reduce((sum, m) => sum + (m.cout || 0), 0);
    const avgCost = total > 0 ? (totalCost / total).toFixed(2) : 0;

    return (
      <div className="report-section">
        <h2>📊 Rapport Résumé</h2>
        <div className="report-grid">
          <div className="report-card">
            <div className="report-label">Total Maintenances</div>
            <div className="report-value">{total}</div>
          </div>
          <div className="report-card success">
            <div className="report-label">Complétées</div>
            <div className="report-value">{completed}</div>
            <div className="report-percent">({total > 0 ? ((completed/total)*100).toFixed(1) : 0}%)</div>
          </div>
          <div className="report-card warning">
            <div className="report-label">En Cours</div>
            <div className="report-value">{inProgress}</div>
            <div className="report-percent">({total > 0 ? ((inProgress/total)*100).toFixed(1) : 0}%)</div>
          </div>
          <div className="report-card info">
            <div className="report-label">Planifiées</div>
            <div className="report-value">{planned}</div>
            <div className="report-percent">({total > 0 ? ((planned/total)*100).toFixed(1) : 0}%)</div>
          </div>
          <div className="report-card danger">
            <div className="report-label">Coût Total</div>
            <div className="report-value">{totalCost.toFixed(2)} TND</div>
          </div>
          <div className="report-card info">
            <div className="report-label">Coût Moyen</div>
            <div className="report-value">{avgCost} TND</div>
          </div>
        </div>
      </div>
    );
  };

  const generateDetailedReport = () => {
    const filtered = getFilteredData();
    return (
      <div className="report-section">
        <h2>📋 Rapport Détaillé</h2>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Véhicule</th>
                <th>Type Maintenance</th>
                <th>Statut</th>
                <th>Coût</th>
                <th>Description</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length > 0 ? (
                filtered.map((m) => (
                  <tr key={m.id}>
                    <td>{new Date(m.datePrevue).toLocaleDateString('fr-FR')}</td>
                    <td><strong>{m.vehicule?.immatriculation || 'N/A'}</strong></td>
                    <td>{m.type}</td>
                    <td>
                      <span className={`badge badge-${m.statut.toLowerCase()}`}>
                        {m.statut === 'PLANIFIEE' && '📅'}
                        {m.statut === 'EN_COURS' && '⚙️'}
                        {m.statut === 'TERMINEE' && '✅'}
                        {' '}{m.statut}
                      </span>
                    </td>
                    <td>{m.cout || 0} TND</td>
                    <td>{m.description || '-'}</td>
                    <td>
                      <button
                        className="btn btn-sm btn-primary"
                        onClick={() => handleEnvoyerRapportPdf(m.id)}
                      >
                        📄 PDF
                      </button>
                      {(m.statut === 'EN_COURS' || m.statut === 'TERMINEE') && (
                        <button
                          className="btn btn-sm btn-info"
                          onClick={() => handleOpenReportModal(m.id)}
                          disabled={sendingReportId === m.id}
                        >
                          {sendingReportId === m.id ? '⏳...' : '📝 Rapport'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7" className="text-center">Aucune maintenance</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  const generateByVehicleReport = () => {
    const filtered = getFilteredData();
    const byVehicle = {};

    filtered.forEach(m => {
      const vehicleId = m.vehicule?.id || 'unknown';
      const vehicleName = m.vehicule?.immatriculation || 'N/A';
      
      if (!byVehicle[vehicleId]) {
        byVehicle[vehicleId] = {
          name: vehicleName,
          total: 0,
          cost: 0,
          statuts: { PLANIFIEE: 0, EN_COURS: 0, TERMINEE: 0 },
          maintenances: []
        };
      }

      byVehicle[vehicleId].total++;
      byVehicle[vehicleId].cost += m.cout || 0;
      byVehicle[vehicleId].statuts[m.statut]++;
      byVehicle[vehicleId].maintenances.push(m);
    });

    return (
      <div className="report-section">
        <h2>🚗 Rapport par Véhicule</h2>
        {Object.values(byVehicle).map((vehicle, idx) => (
          <div key={idx} className="vehicle-report">
            <h3>{vehicle.name}</h3>
            <div className="vehicle-stats">
              <div className="stat-item">
                <span>Total:</span>
                <strong>{vehicle.total}</strong>
              </div>
              <div className="stat-item">
                <span>Coût:</span>
                <strong>{vehicle.cost.toFixed(2)} TND</strong>
              </div>
              <div className="stat-item">
                <span>Planifiées:</span>
                <strong>{vehicle.statuts.PLANIFIEE}</strong>
              </div>
              <div className="stat-item">
                <span>En Cours:</span>
                <strong>{vehicle.statuts.EN_COURS}</strong>
              </div>
              <div className="stat-item">
                <span>Terminées:</span>
                <strong>{vehicle.statuts.TERMINEE}</strong>
              </div>
            </div>
            <table className="mini-table">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Date</th>
                  <th>Statut</th>
                  <th>Coût</th>
                </tr>
              </thead>
              <tbody>
                {vehicle.maintenances.map((m) => (
                  <tr key={m.id}>
                    <td>{m.type}</td>
                    <td>{new Date(m.datePrevue).toLocaleDateString('fr-FR')}</td>
                    <td>
                      <span className={`badge badge-${m.statut.toLowerCase()}`}>
                        {m.statut}
                      </span>
                    </td>
                    <td>{m.cout || 0} TND</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    );
  };

  if (loading) return <div className="loading">Chargement...</div>;

  return (
    <div className="chef-container">
      <div className="section-header">
        <h1>📊 Rapports de Maintenance</h1>
        <p>Consultez les rapports de maintenance</p>
      </div>

      {/* Sélecteurs */}
      <div className="filters-container">
        <div className="filter-group">
          <label>Période:</label>
          <select value={period} onChange={(e) => setPeriod(e.target.value)}>
            <option value="month">Ce mois</option>
            <option value="quarter">Ce trimestre</option>
            <option value="year">Cette année</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Type de Rapport:</label>
          <select value={reportType} onChange={(e) => setReportType(e.target.value)}>
            <option value="summary">📊 Résumé</option>
            <option value="detailed">📋 Détaillé</option>
            <option value="by-vehicle">🚗 Par Véhicule</option>
          </select>
        </div>

        <button 
          className="btn btn-primary"
          onClick={() => {
            const content = document.querySelector('.reports-container').innerHTML;
            const printWindow = window.open('', '', 'width=900,height=600');
            printWindow.document.write('<html><head><title>Rapport Maintenance</title>');
            printWindow.document.write('<link rel="stylesheet" href="./styles/chef.css">');
            printWindow.document.write('</head><body>');
            printWindow.document.write(content);
            printWindow.document.write('</body></html>');
            printWindow.print();
          }}
        >
          🖨️ Imprimer
        </button>
      </div>

      {/* Rapports */}
      <div className="reports-container">
        {reportType === 'summary' && generateSummaryReport()}
        {reportType === 'detailed' && generateDetailedReport()}
        {reportType === 'by-vehicle' && generateByVehicleReport()}
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

export default OperateurMaintenanceReports;
