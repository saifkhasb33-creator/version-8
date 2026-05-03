import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getUtilisateur, createUtilisateur, updateUtilisateur } from '../../services/utilisateur';
import { getParcs } from '../../services/parc';

function UserForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [parcs, setParcs] = useState([]);
  const [formData, setFormData] = useState({
    // Communs
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    role: 'CHAUFFEUR',
    actif: true,
    motDePasse: '',
    // Chauffeur
    numeroPermis: '',
    dateExpirationPermis: '',
    disponible: 'disponible',
    id_parc: '',
    // Chef de parc
    dateEmbauche: '',
    zoneAffectation: '',
    // Opérateur maintenance
    specialite: '',
    niveau: 'intermédiaire',
    id_garage: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loadingParcs, setLoadingParcs] = useState(false);

  const isEditMode = !!id;

  // Charger la liste des parcs
  useEffect(() => {
    const fetchParcs = async () => {
      setLoadingParcs(true);
      try {
        const response = await getParcs();
        setParcs(response.data);
      } catch (err) {
        console.error('Erreur chargement parcs', err);
      } finally {
        setLoadingParcs(false);
      }
    };
    fetchParcs();
  }, []);

  // Charger les données de l'utilisateur en mode édition
  useEffect(() => {
    if (isEditMode) {
      const fetchUser = async () => {
        try {
          const response = await getUtilisateur(id);
          const user = response.data;
          setFormData({
            // Communs
            nom: user.nom || '',
            prenom: user.prenom || '',
            email: user.email || '',
            telephone: user.telephone || '',
            role: user.role || 'CHAUFFEUR',
            actif: user.actif ?? true,
            motDePasse: '',
            // Chauffeur
            numeroPermis: user.numeroPermis || '',
            dateExpirationPermis: user.dateExpirationPermis || '',
            disponible: user.disponible || 'disponible',
            id_parc: user.id_parc || '',
            // Chef de parc
            dateEmbauche: user.dateEmbauche || '',
            zoneAffectation: user.zoneAffectation || '',
            // Opérateur maintenance
            specialite: user.specialite || '',
            niveau: user.niveau || 'intermédiaire',
            id_garage: user.id_garage || '',
          });
        } catch (err) {
          setError('Erreur lors du chargement de l’utilisateur');
        }
      };
      fetchUser();
    }
  }, [id, isEditMode]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    // Validation simple
    if (!formData.nom || !formData.prenom || !formData.email) {
      setError('Veuillez remplir tous les champs obligatoires');
      setLoading(false);
      return;
    }
    if (!isEditMode && !formData.motDePasse) {
      setError('Le mot de passe est obligatoire');
      setLoading(false);
      return;
    }

    try {
      if (isEditMode) {
        await updateUtilisateur(id, formData);
        setSuccess('Utilisateur modifié avec succès');
      } else {
        await createUtilisateur(formData);
        setSuccess('Utilisateur créé avec succès');
        // Réinitialiser le formulaire
        setFormData({
          nom: '', prenom: '', email: '', telephone: '', role: 'CHAUFFEUR', actif: true, motDePasse: '',
          numeroPermis: '', dateExpirationPermis: '', disponible: 'disponible', id_parc: '',
          dateEmbauche: '', zoneAffectation: '',
          specialite: '', niveau: 'intermédiaire', id_garage: '',
        });
      }
      setTimeout(() => {
        navigate('/admin/users');
      }, 1500);
    } catch (err) {
      const message = err.response?.data?.error || err.message;
      if (message.includes('email') || message.includes('duplicate')) {
        setError('Cet email est déjà utilisé par un autre utilisateur.');
      } else {
        setError('Erreur lors de l’enregistrement : ' + message);
      }
    } finally {
      setLoading(false);
    }
  };

  // Liste des rôles disponibles
  const roles = [
    { value: 'CHAUFFEUR', label: 'Chauffeur' },
    { value: 'CHEF', label: 'Chef de parc' },
    { value: 'OPERATEUR_MAINTENANCE', label: 'Opérateur maintenance' },
    { value: 'ADMIN', label: 'Administrateur' },
  ];

  return (
    <div className="user-form-container">
      <div className="page-header">
        <h2>{isEditMode ? 'Modifier' : 'Ajouter'} un utilisateur</h2>
        <button className="btn-secondary" onClick={() => navigate('/admin/users')}>
          ← Retour
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form onSubmit={handleSubmit} className="user-form">
        {/* CHAMPS COMMUNS */}
        <div className="form-row">
          <div className="form-group">
            <label>Nom *</label>
            <input type="text" name="nom" value={formData.nom} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Prénom *</label>
            <input type="text" name="prenom" value={formData.prenom} onChange={handleChange} required />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Email *</label>
            <input type="email" name="email" value={formData.email} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Téléphone</label>
            <input type="tel" name="telephone" value={formData.telephone} onChange={handleChange} />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label>Rôle *</label>
            <select name="role" value={formData.role} onChange={handleChange} required>
              {roles.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group checkbox-group">
            <label>
              <input type="checkbox" name="actif" checked={formData.actif} onChange={handleChange} />
              Compte actif
            </label>
          </div>
        </div>

        {!isEditMode && (
          <div className="form-group">
            <label>Mot de passe *</label>
            <input type="password" name="motDePasse" value={formData.motDePasse} onChange={handleChange} required />
          </div>
        )}

        {/* CHAMPS SPÉCIFIQUES : CHAUFFEUR */}
        {formData.role === 'CHAUFFEUR' && (
          <div className="role-fields">
            <h3>Informations chauffeur</h3>
            <div className="form-row">
              <div className="form-group">
                <label>Numéro de permis</label>
                <input type="text" name="numeroPermis" value={formData.numeroPermis} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label>Date expiration permis</label>
                <input type="date" name="dateExpirationPermis" value={formData.dateExpirationPermis} onChange={handleChange} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Disponibilité</label>
                <select name="disponible" value={formData.disponible} onChange={handleChange}>
                  <option value="disponible">Disponible</option>
                  <option value="occupe">En mission</option>
                  <option value="congé">En congé</option>
                </select>
              </div>
              <div className="form-group">
                <label>Parc *</label>
                <select name="id_parc" value={formData.id_parc} onChange={handleChange} required>
                  <option value="">-- Sélectionner un parc --</option>
                  {loadingParcs ? (
                    <option disabled>Chargement...</option>
                  ) : (
                    parcs.map((parc) => (
                      <option key={parc.id} value={parc.id}>
                        {parc.nom}
                      </option>
                    ))
                  )}
                </select>
              </div>
            </div>
          </div>
        )}

        {/* CHAMPS SPÉCIFIQUES : CHEF DE PARC */}
        {formData.role === 'CHEF' && (
          <div className="role-fields">
            <h3>Informations chef de parc</h3>
            <div className="form-row">
              <div className="form-group">
                <label>Date d'embauche</label>
                <input type="date" name="dateEmbauche" value={formData.dateEmbauche} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label>Zone d'affectation</label>
                <input type="text" name="zoneAffectation" value={formData.zoneAffectation} onChange={handleChange} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Parc *</label>
                <select name="id_parc" value={formData.id_parc} onChange={handleChange} required>
                  <option value="">-- Sélectionner un parc --</option>
                  {loadingParcs ? (
                    <option disabled>Chargement...</option>
                  ) : (
                    parcs.map((parc) => (
                      <option key={parc.id} value={parc.id}>
                        {parc.nom}
                      </option>
                    ))
                  )}
                </select>
              </div>
            </div>
          </div>
        )}

        {/* CHAMPS SPÉCIFIQUES : OPÉRATEUR MAINTENANCE */}
        {formData.role === 'OPERATEUR_MAINTENANCE' && (
          <div className="role-fields">
            <h3>Informations opérateur maintenance</h3>
            <div className="form-row">
              <div className="form-group">
                <label>Spécialité</label>
                <input type="text" name="specialite" value={formData.specialite} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label>Date d'embauche</label>
                <input type="date" name="dateEmbauche" value={formData.dateEmbauche} onChange={handleChange} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Niveau d'expertise</label>
                <select name="niveau" value={formData.niveau} onChange={handleChange}>
                  <option value="débutant">Débutant</option>
                  <option value="intermédiaire">Intermédiaire</option>
                  <option value="expert">Expert</option>
                </select>
              </div>
              <div className="form-group">
                <label>Garage associé (ID)</label>
                <input type="number" name="id_garage" value={formData.id_garage} onChange={handleChange} />
              </div>
            </div>
          </div>
        )}

        {/* BOUTONS */}
        <div className="form-actions">
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Enregistrement...' : isEditMode ? 'Modifier' : 'Créer'}
          </button>
          <button type="button" onClick={() => navigate('/admin/users')} className="btn-secondary">
            Annuler
          </button>
        </div>
      </form>
    </div>
  );
}

export default UserForm;