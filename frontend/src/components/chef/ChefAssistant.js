import React, { useState, useEffect, useRef } from 'react';
import OllamaService from '../../services/ollamaService';
import { getChefDashboardStats } from '../../services/stats';
import { useNotification } from '../../context/NotificationContext';
import '../../styles/chatbot.css';

function ChefAssistant({ user }) {
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: 'bot',
      text: '🤖 Bonjour! Je suis votre assistant IA. Je peux vous aider avec vos données de parc, véhicules, chauffeurs, missions, etc.',
      timestamp: new Date()
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [connected, setConnected] = useState(false);
  const [stats, setStats] = useState({});
  const messagesEndRef = useRef(null);
  const { showError, showSuccess } = useNotification();

  // Vérifier connexion Ollama au démarrage
  useEffect(() => {
    checkOllamaConnection();
    loadStats();
  }, []);

  // Scroll vers le dernier message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const checkOllamaConnection = async () => {
    const isConnected = await OllamaService.testConnection();
    setConnected(isConnected);
    if (!isConnected) {
      setMessages(prev => [...prev, {
        id: prev.length + 1,
        type: 'error',
        text: '⚠️ Ollama n\'est pas accessible. Assurez-vous qu\'Ollama est démarré sur http://localhost:11434',
        timestamp: new Date()
      }]);
    } else {
      showSuccess('✅ Ollama connecté avec succès!');
    }
  };

  const loadStats = async () => {
    try {
      const statsData = await getChefDashboardStats(user?.parcId);
      setStats(statsData);
    } catch (error) {
      console.error('Erreur chargement stats:', error);
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    
    if (!inputValue.trim()) return;
    
    if (!connected) {
      showError('❌ Ollama non connecté. Veuillez le démarrer d\'abord.');
      return;
    }

    // Ajouter le message utilisateur
    const userMessage = {
      id: messages.length + 1,
      type: 'user',
      text: inputValue,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setLoading(true);

    try {
      // Appeler l'IA avec contexte
      const response = await OllamaService.askAssistant(inputValue, {
        stats: stats,
        userId: user?.id,
        parcId: user?.parcId
      });

      const botMessage = {
        id: messages.length + 2,
        type: 'bot',
        text: response,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, botMessage]);
    } catch (error) {
      const errorMessage = {
        id: messages.length + 2,
        type: 'error',
        text: `❌ Erreur: ${error.message}`,
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMessage]);
      showError(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="chef-assistant-container">
      {/* Bouton flottant */}
      <button
        className="assistant-toggle-btn"
        onClick={() => setIsOpen(!isOpen)}
        title={connected ? '🤖 Assistant IA Actif' : '⚠️ Assistant Hors Ligne'}
      >
        {connected ? '💬' : '❌'}
      </button>

      {/* Fenêtre ChatBot */}
      {isOpen && (
        <div className="assistant-window">
          {/* Header */}
          <div className="assistant-header">
            <h3>🤖 Assistant IA - Chef de Parc</h3>
            <button
              className="close-btn"
              onClick={() => setIsOpen(false)}
            >
              ✕
            </button>
          </div>

          {/* Statut connexion */}
          <div className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
            {connected ? (
              <span>✅ Ollama Connecté</span>
            ) : (
              <span>❌ Ollama Hors Ligne - Démarrez Ollama</span>
            )}
          </div>

          {/* Messages */}
          <div className="messages-container">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`message ${msg.type}`}
              >
                <div className="message-content">
                  {msg.type === 'user' && <span className="user-icon">👤</span>}
                  {msg.type === 'bot' && <span className="bot-icon">🤖</span>}
                  {msg.type === 'error' && <span className="error-icon">⚠️</span>}
                  <div className="message-text">{msg.text}</div>
                </div>
                <small className="message-time">
                  {msg.timestamp.toLocaleTimeString('fr-FR', {
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </small>
              </div>
            ))}
            {loading && (
              <div className="message bot">
                <div className="message-content">
                  <span className="bot-icon">🤖</span>
                  <div className="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Form */}
          <form onSubmit={handleSendMessage} className="input-form">
            <input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="Posez une question sur votre parc..."
              disabled={!connected || loading}
              className="input-field"
            />
            <button
              type="submit"
              disabled={!connected || loading || !inputValue.trim()}
              className="send-btn"
            >
              {loading ? '⏳' : '➤'}
            </button>
          </form>

          {/* Suggestions */}
          <div className="suggestions">
            <small>💡 Exemples de questions:</small>
            <div className="suggestion-chips">
              {[
                'Quel est l\'état de ma flotte?',
                'Quels véhicules nécessitent maintenance?',
                'Affiche les chauffeurs disponibles',
                'Coût total des amendes ce mois-ci?'
              ].map((suggestion, idx) => (
                <button
                  key={idx}
                  className="suggestion-chip"
                  onClick={() => {
                    setInputValue(suggestion);
                    setTimeout(() => {
                      handleSendMessage({ preventDefault: () => {} });
                    }, 100);
                  }}
                  disabled={!connected || loading}
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ChefAssistant;
