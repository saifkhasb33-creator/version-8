import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
 
const PrivateRoute = ({ children }) => {
  const { user, loading } = useAuth();
 
  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        fontSize: '1.1rem',
        color: '#64748b'
      }}>
        Chargement...
      </div>
    );
  }
 
  if (!user) {
    return <Navigate to="/login" replace />;
  }
 
  return children;
};
 
export default PrivateRoute;