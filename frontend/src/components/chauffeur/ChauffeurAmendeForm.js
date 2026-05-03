import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import { createAmende } from '../../services/amende';
import '../styles/form.css';

function ChauffeurAmendeForm() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showSuccess, showError } = useNotification();
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState({
    montant: '',
    motif: '',
    date: new Date().toISOString().split('T')[0],
    lieu: '',
    description: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.montant || !formData.motif) {
      showError('Veuillez remplir tous les champs obligatoires');
      return;
    }

    setLoading(true);
    try {
      const amendeData = {
        // ✅ Noms exacts des champs du DTO backend AmendeDTO
        montant:        parseFloat(formData.montant),
        motif:          formData.motif,
        dateInfraction: formData.date,             // ← était "dateAmende", doit être "dateInfraction"
        lieuInfraction: formData.lieu || 'Non précisé', // ← champ ajouté, absent dans l'original
        description:    formData.description,
        chauffeurId:    user.id,
        statut:         'SIGNALÉE'
        // vehiculeId intentionnellement absent : géré côté backend via fallback
      };

      await createAmende(amendeData);
      showSuccess('Amende signalée avec succès. Le chef de parc a été notifié.');
      setTimeout(() => navigate('/chauffeur/dashboard'), 1500);
    } catch (err) {
      console.error('Erreur:', err);
      showError('Erreur: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-container">
      <div className="page-header">
        <h2>⚠️ Déclarer une Amende</h2>
        <button className="btn-secondary" onClick={() => navigate('/chauffeur/dashboard')}>← Retour</button>
      </div>

      <form onSubmit={handleSubmit} className="user-form">
        <div className="form-group">
          <label htmlFor="motif">Motif de l'amende *</label>
          <select
            id="motif"
            name="motif"
            value={formData.motif}
            onChange={handleChange}
            required
          >
            <option value="">-- Sélectionner un motif --</option>
            <option value="EXCÈS_VITESSE">Excès de vitesse</option>
            <option value="FEUX_ROUGE">Passage au feu rouge</option>
            <option value="PARKING_INTERDIT">Stationnement interdit</option>
            <option value="CEINTURE_SECURITÉ">Oubli ceinture de sécurité</option>
            <option value="TELEPHONE">Utilisation du téléphone</option>
            <option value="INSPECTION_TECHNIQUE">Défaut d'inspection technique</option>
            <option value="ASSURANCE">Défaut d'assurance</option>
            <option value="AUTRE">Autre</option>
          </select>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="montant">Montant (TND) *</label>
            <input
              type="number"
              id="montant"
              name="montant"
              value={formData.montant}
              onChange={handleChange}
              placeholder="Ex: 50.00"
              step="0.01"
              min="0"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="date">Date de l'amende *</label>
            <input
              type="date"
              id="date"
              name="date"
              value={formData.date}
              onChange={handleChange}
              required
            />
          </div>
        </div>

        {/* ✅ Champ lieu ajouté — correspond à lieuInfraction dans AmendeDTO */}
        <div className="form-group">
          <label htmlFor="lieu">Lieu de l'infraction</label>
          <input
            type="text"
            id="lieu"
            name="lieu"
            value={formData.lieu}
            onChange={handleChange}
            placeholder="Ex: Avenue Habib Bourguiba, Tunis"
          />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description (facultatif)</label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
            placeholder="Décrivez les circonstances de l'amende..."
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Signalement...' : "Signaler l'amende"}
          </button>
          <button type="button" onClick={() => navigate('/chauffeur/dashboard')} className="btn-secondary">
            Annuler
          </button>
        </div>
      </form>
    </div>
  );
}

export default ChauffeurAmendeForm;