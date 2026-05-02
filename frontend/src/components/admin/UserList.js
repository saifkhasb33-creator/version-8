import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getUtilisateurs, deleteUtilisateur } from '../../services/utilisateur';

function UserList() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('');

  useEffect(() => { fetchUsers(); }, []);

  const fetchUsers = async () => {
    try {
      const res = await getUtilisateurs();
      setUsers(res.data);
    } catch (err) { console.error(err); } finally { setLoading(false); }
  };

  const handleDelete = async (id, email) => {
    if (window.confirm(`Supprimer ${email} ?`)) {
      try {
        await deleteUtilisateur(id);
        setUsers(users.filter(u => u.id !== id));
      } catch (err) { alert('Erreur suppression'); }
    }
  };

  const getRoleLabel = (role) => {
    const roles = { ADMIN: 'Admin', CHEF: 'Chef de parc', CHAUFFEUR: 'Chauffeur', OPERATEUR_MAINTENANCE: 'Opérateur maint.' };
    return roles[role] || role;
  };

  const getRoleClass = (role) => {
    const classes = { ADMIN: 'badge-admin', CHEF: 'badge-chef', CHAUFFEUR: 'badge-chauffeur', OPERATEUR_MAINTENANCE: 'badge-operateur' };
    return classes[role] || 'badge-default';
  };

  const filtered = users.filter(u => {
    const matchSearch = u.nom?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        u.prenom?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        u.email?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchRole = roleFilter === '' || u.role === roleFilter;
    return matchSearch && matchRole;
  });

  if (loading) return <div className="loading">Chargement...</div>;

  return (
    <div className="user-list-container">
      <div className="page-header">
        <h2>👥 Utilisateurs</h2>
        <button className="btn-primary" onClick={() => navigate('/admin/users/new')}>+ Ajouter</button>
      </div>

      <div className="filters-bar">
        <div className="search-box">
          <input type="text" placeholder="Rechercher..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
        </div>
        <div className="filter-select">
          <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">Tous les rôles</option>
            <option value="ADMIN">Admin</option>
            <option value="CHEF">Chef de parc</option>
            <option value="CHAUFFEUR">Chauffeur</option>
            <option value="OPERATEUR_MAINTENANCE">Opérateur maintenance</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr><th>ID</th><th>Nom complet</th><th>Email</th><th>Téléphone</th><th>Rôle</th><th>Statut</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {filtered.map(u => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td><strong>{u.prenom} {u.nom}</strong></td>
                <td>{u.email}</td>
                <td>{u.telephone || '-'}</td>
                <td><span className={`role-badge ${getRoleClass(u.role)}`}>{getRoleLabel(u.role)}</span></td>
                <td><span className={`status-badge ${u.actif ? 'active' : 'inactive'}`}>{u.actif ? 'Actif' : 'Inactif'}</span></td>
                <td className="actions">
                  <Link to={`/admin/users/edit/${u.id}`} className="btn-edit">✏️</Link>
                  <button onClick={() => handleDelete(u.id, u.email)} className="btn-delete">🗑️</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="stats-footer"><p>Total : {filtered.length} utilisateur(s)</p></div>
    </div>
  );
}

export default UserList;