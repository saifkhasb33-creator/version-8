import axios from 'axios';

const BACKEND_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

class OllamaService {
  static async askAssistant(userMessage, context = {}) {
    try {
      const response = await axios.post(
        `${BACKEND_URL}/api/assistant/ask`,
        { message: userMessage, context: context },
        { timeout: 60000 }
      );
      return response.data.response || 'Pas de réponse';
    } catch (error) {
      console.error('❌ Erreur ChatBot:', error);
      throw new Error(error.response?.data?.message || 'Erreur de communication avec l\'assistant IA');
    }
  }

  static async testConnection() {
    try {
      const response = await axios.get(
        `${BACKEND_URL}/api/assistant/health`,
        { timeout: 8000 }
      );
      return response.data.success === true;
    } catch (error) {
      console.error('❌ Backend/Ollama non accessible:', error.message);
      return false;
    }
  }

  static async getAvailableModels() {
    try {
      const response = await axios.get(`${BACKEND_URL}/api/assistant/models`, {
        timeout: 8000
      });
      return response.data.models || [];
    } catch (error) {
      console.error('Erreur récupération modèles:', error);
      return [];
    }
  }
}

export default OllamaService;
