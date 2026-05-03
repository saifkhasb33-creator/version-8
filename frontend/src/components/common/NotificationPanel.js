import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getNotifications,
  marquerLu,
  marquerTousLus,
  supprimerNotification
} from '../../services/notification';
import { useNotification as useAppNotification } from '../../context/NotificationContext';
import '../../styles/notifications.css';

const getNotificationIcon = (type) => {
  const icons = {
    MISSION_ASSIGNÉE: '📋',
    MISSION_MODIFIÉE: '🔄',
    CONGÉ_VALIDÉ: '✅',
    CONGÉ_REFUSÉ: '❌',
    MAINTENANCE_PROGRAMMÉE: '🔧',
    ALERTE_ASSURANCE: '📅',
    ALERTE_VIDANGE: '⛽',
    RAPPORT_MAINTENANCE: '📄',
    default: '🔔'
  };
  return icons[type] || icons.default;
};

const NotificationPanel = ({ user }) => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { showSuccess, showError } = useAppNotification();

  const load = async () => {
    try {
      const res = await getNotifications();
      setNotifications(res.data || []);
    } catch (e) {
      console.error(e);
      showError('Erreur chargement notifications');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleMarquerLu = async (id) => {
    try {
      await marquerLu(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, lue: true } : n));
    } catch (e) {
      showError('Erreur mise à jour notification');
    }
  };

  const handleMarquerTousLus = async () => {
    try {
    await marquerTousLus();
      setNotifications(prev => prev.map(n => ({ ...n, lue: true })));
      showSuccess('✅ Toutes les notifications marquées comme lues');
    } catch (e) {
      showError('Erreur');
    }
  };

  const handleSupprimer = async (id) => {
    try {
      await supprimerNotification(id);
      setNotifications(prev => prev.filter(n => n.id !== id));
    } catch (e) {
      showError('Erreur suppression');
    }
  };

  const handleRedirection = (notification) => {
    handleMarquerLu(notification.id);

    if (notification.type === 'RAPPORT_MAINTENANCE') {
      navigate('/chef/rapports-maintenance');
      return;
    }

    if (notification.lienRedirection) {
      navigate(notification.lienRedirection);
    }
  };

  const nonLues = notifications.filter(n => !n.lue);
  const lues = notifications.filter(n => n.lue);

  return (
    <div className="notification-panel">
      <div className="notification-header">
        <h3>🔔 Notifications ({nonLues.length})</h3>
        {nonLues.length > 0 && (
          <button className="btn-link" onClick={handleMarquerTousLus}>
            Tout marquer comme lu
          </button>
        )}
      </div>

      {loading ? (
        <div className="loading">Chargement...</div>
      ) : notifications.length === 0 ? (
        <div className="empty">Aucune notification</div>
      ) : (
        <div className="notification-list">
          {nonLues.map(n => (
            <div key={n.id} className="notification-item unread" onClick={() => handleRedirection(n)}>
              <span className="notification-icon">{getNotificationIcon(n.type)}</span>
              <div className="notification-body">
                <strong>{n.titre}</strong>
                <p>{n.message}</p>
                <small>{new Date(n.dateEnvoi).toLocaleString('fr-FR')}</small>
              </div>
              <button className="btn-icon" onClick={(e) => { e.stopPropagation(); handleSupprimer(n.id); }}>🗑️</button>
            </div>
          ))}
          {lues.map(n => (
            <div key={n.id} className="notification-item" onClick={() => handleRedirection(n)}>
              <span className="notification-icon">{getNotificationIcon(n.type)}</span>
              <div className="notification-body">
                <strong>{n.titre}</strong>
                <p>{n.message}</p>
                <small>{new Date(n.dateEnvoi).toLocaleString('fr-FR')}</small>
              </div>
              <button className="btn-icon" onClick={(e) => { e.stopPropagation(); handleSupprimer(n.id); }}>🗑️</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default NotificationPanel;
