import axios from 'axios';

// Configuration de l'API Ollama et Backend
const OLLAMA_URL = 'http://localhost:11434/api/generate';
const BACKEND_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

class OllamaService {
  /**
   * Envoyer une requête au ChatBot IA avec contexte base de données
   * @param {string} userMessage - Message utilisateur
   * @param {object} context - Contexte (stats parc, véhicules, etc)
   * @returns {Promise<string>} - Réponse IA
   */
  static async askAssistant(userMessage, context = {}) {
    try {
      // Appeler notre backend pour intégrer les données
      const response = await axios.post(
        `${BACKEND_URL}/api/assistant/ask`,
        {
          message: userMessage,
          context: context
        },
        {
          timeout: 30000 // 30 secondes timeout
        }
      );
      
      return response.data.response || 'Pas de réponse';
    } catch (error) {
      console.error('❌ Erreur ChatBot:', error);
      throw new Error(error.response?.data?.message || 'Erreur de communication avec l\'assistant IA');
    }
  }

  /**
   * Tester la connexion Ollama
   */
  static async testConnection() {
    try {
      const response = await axios.post(
        OLLAMA_URL,
        {
          model: 'mistral',
          prompt: 'test',
          stream: false
        },
        { timeout: 5000 }
      );
      return response.data !== undefined;
    } catch (error) {
      console.error('❌ Ollama non accessible:', error.message);
      return false;
    }
  }

  /**
   * Obtenir les modèles disponibles
   */
  static async getAvailableModels() {
    try {
      const response = await axios.get(`${OLLAMA_URL.replace('/generate', '/tags')}`);
      return response.data.models || [];
    } catch (error) {
      console.error('Erreur récupération modèles:', error);
      return [];
    }
  }
}

export default OllamaService;
