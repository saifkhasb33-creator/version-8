import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getUtilisateur, createUtilisateur, updateUtilisateur } from '../../services/utilisateur';

function UserForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    nom: '', prenom: '', email: '', telephone: '', motDePasse: '',
    role: 'CHAUFFEUR', actif: true,
    // Chauffeur
    numeroPermis: '', dateExpirationPermis: '', disponible: 'disponible', id_parc: '',
    // Chef
    dateEmbauche: '', zoneAffectation: '',
    // Opérateur
    specialite: '', id_garage: ''
  });
  const [loading, setLoading] = useState(false);
  const isEditMode = !!id;

  useEffect(() => {
    if (isEditMode) {
      const fetchUser = async () => {
        try {
          const res = await getUtilisateur(id);
          setFormData(res.data);
        } catch (err) { console.error(err); }
      };
      fetchUser();
    }
  }, [id]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isEditMode) await updateUtilisateur(id, formData);
      else await createUtilisateur(formData);
      navigate('/admin/users');
    } catch (err) { alert('Erreur enregistrement'); } finally { setLoading(false); }
  };

  const roles = [
    { value: 'ADMIN', label: 'Administrateur' },
    { value: 'CHEF', label: 'Chef de parc' },
    { value: 'CHAUFFEUR', label: 'Chauffeur' },
    { value: 'OPERATEUR_MAINTENANCE', label: 'Opérateur maintenance' }
  ];

  return (
    <div className="user-form-container">
      <div className="page-header">
        <h2>{isEditMode ? 'Modifier' : 'Ajouter'} un utilisateur</h2>
        <button className="btn-secondary" onClick={() => navigate('/admin/users')}>← Retour</button>
      </div>
      <form onSubmit={handleSubmit} className="user-form">
        <div className="form-row">
          <div className="form-group"><label>Nom *</label><input type="text" name="nom" value={formData.nom} onChange={handleChange} required /></div>
          <div className="form-group"><label>Prénom *</label><input type="text" name="prenom" value={formData.prenom} onChange={handleChange} required /></div>
        </div>
        <div className="form-row">
          <div className="form-group"><label>Email *</label><input type="email" name="email" value={formData.email} onChange={handleChange} required /></div>
          <div className="form-group"><label>Téléphone</label><input type="tel" name="telephone" value={formData.telephone} onChange={handleChange} /></div>
        </div>
        {!isEditMode && (
          <div className="form-group"><label>Mot de passe</label><input type="password" name="motDePasse" value={formData.motDePasse} onChange={handleChange} /></div>
        )}
        <div className="form-row">
          <div className="form-group"><label>Rôle</label><select name="role" value={formData.role} onChange={handleChange}>{roles.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}</select></div>
          <div className="form-group checkbox-group"><label><input type="checkbox" name="actif" checked={formData.actif} onChange={handleChange} /> Actif</label></div>
        </div>

        {/* Champs spécifiques Chauffeur */}
        {formData.role === 'CHAUFFEUR' && (
          <div className="role-fields">
            <h3>Chauffeur</h3>
            <div className="form-row">
              <div className="form-group"><label>Numéro permis</label><input type="text" name="numeroPermis" value={formData.numeroPermis} onChange={handleChange} /></div>
              <div className="form-group"><label>Expiration permis</label><input type="date" name="dateExpirationPermis" value={formData.dateExpirationPermis} onChange={handleChange} /></div>
            </div>
            <div className="form-row">
              <div className="form-group"><label>Disponibilité</label><select name="disponible" value={formData.disponible} onChange={handleChange}><option value="disponible">Disponible</option><option value="occupe">En mission</option><option value="conge">Congé</option></select></div>
              <div className="form-group"><label>Parc</label><input type="number" name="id_parc" value={formData.id_parc} onChange={handleChange} placeholder="ID parc" /></div>
            </div>
          </div>
        )}

        {/* Champs spécifiques Chef */}
        {formData.role === 'CHEF' && (
          <div className="role-fields">
            <h3>Chef de parc</h3>
            <div className="form-row">
              <div className="form-group"><label>Date embauche</label><input type="date" name="dateEmbauche" value={formData.dateEmbauche} onChange={handleChange} /></div>
              <div className="form-group"><label>Zone affectation</label><input type="text" name="zoneAffectation" value={formData.zoneAffectation} onChange={handleChange} /></div>
            </div>
            <div className="form-row">
              <div className="form-group"><label>Parc</label><input type="number" name="id_parc" value={formData.id_parc} onChange={handleChange} placeholder="ID parc" /></div>
            </div>
          </div>
        )}

        {/* Champs spécifiques Opérateur */}
        {formData.role === 'OPERATEUR_MAINTENANCE' && (
          <div className="role-fields">
            <h3>Opérateur maintenance</h3>
            <div className="form-row">
              <div className="form-group"><label>Spécialité</label><input type="text" name="specialite" value={formData.specialite} onChange={handleChange} /></div>
              <div className="form-group"><label>Date embauche</label><input type="date" name="dateEmbauche" value={formData.dateEmbauche} onChange={handleChange} /></div>
            </div>
            <div className="form-group"><label>Garage</label><input type="number" name="id_garage" value={formData.id_garage} onChange={handleChange} placeholder="ID garage" /></div>
          </div>
        )}

        <div className="form-actions">
          <button type="submit" disabled={loading} className="btn-primary">{loading ? 'Enregistrement...' : (isEditMode ? 'Modifier' : 'Créer')}</button>
          <button type="button" onClick={() => navigate('/admin/users')} className="btn-secondary">Annuler</button>
        </div>
      </form>
    </div>
  );
}

export default UserForm;