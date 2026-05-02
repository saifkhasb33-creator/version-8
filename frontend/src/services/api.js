import axios from 'axios';
 
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
});
 
// ── Intercepteur requête : ajouter le token Bearer ──────────────────────────
api.interceptors.request.use(config => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user && user.token) {
      config.headers.Authorization = `Bearer ${user.token}`;
    }
  } catch (e) {
    // localStorage corrompu → ignorer
  }
  return config;
});
 
// ── Intercepteur réponse : gérer les erreurs d'authentification ─────────────
api.interceptors.response.use(
  response => response,
  error => {
    const status  = error.response?.status;
    const url     = error.config?.url || '';
 
    const isLoginRequest       = url.includes('/auth/login');
    const isNotificationRequest = url.includes('/notifications');
 
    // ⚠️ Déconnecter SEULEMENT si :
    //  - statut 401 (non authentifié)
    //  - ce n'est PAS une requête de login
    //  - ce n'est PAS une requête de notification (qui peut échouer normalement)
    if (status === 401 && !isLoginRequest && !isNotificationRequest) {
      console.error('❌ Session expirée — déconnexion');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
 
    // ⚠️ Ne JAMAIS déconnecter sur 403 (accès refusé ≠ session expirée)
 
    return Promise.reject(error);
  }
);
 
export default api;