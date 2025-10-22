import React, { useState } from 'react';

function QuickOrders() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const createQuickOrders = async () => {
    setLoading(true);
    setMessage('');

    try {
      const token = localStorage.getItem('jwt');
      const userId = localStorage.getItem('userId');

      if (!token || !userId) {
        setMessage('❌ Erreur: Utilisateur non connecté');
        setLoading(false);
        return;
      }

      // Appeler l'endpoint de création d'ordres seed directement dans le matching-service
      const response = await fetch(`${process.env.REACT_APP_API_URL}/debug/bookorder/seed-orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'userId': userId
        }
      });

      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          setMessage(`✅ Succès! ${data.ordersCreated} ordres seed créés: AAPL (10@100$), MSFT (10@200$), TSLA (15@150$), GOOG (17@1200$)`);
        } else {
          setMessage(`❌ Erreur: ${data.message}`);
        }
      } else {
        const errorText = await response.text();
        setMessage(`❌ Erreur HTTP ${response.status}: ${errorText}`);
      }

    } catch (err) {
      setMessage(`❌ Erreur inattendue: ${err.message}`);
    }

    setLoading(false);
  };

  return (
    <div style={{
      margin: '1rem 0',
      padding: '1rem',
      border: '1px solid #ddd',
      borderRadius: '5px',
      backgroundColor: '#f9f9f9'
    }}>
      <h4>🚀 Ordres Seed de Test</h4>
      <p style={{ fontSize: '0.9em', color: '#666', margin: '0.5rem 0' }}>
        Créer instantanément des ordres de vente seed (identiques à la migration V5):
        <br />• AAPL: 10 actions à 100$ • MSFT: 10 actions à 200$ • TSLA: 15 actions à 150$ • GOOG: 17 actions à 1200$
      </p>

      <button
        onClick={createQuickOrders}
        disabled={loading}
        style={{
          backgroundColor: loading ? '#ccc' : '#007bff',
          color: 'white',
          border: 'none',
          padding: '0.75rem 1.5rem',
          borderRadius: '4px',
          cursor: loading ? 'not-allowed' : 'pointer',
          fontWeight: 'bold',
          width: '100%',
          marginBottom: '0.5rem'
        }}
      >
        {loading ? '⏳ Création des ordres...' : '🔥 Créer 4 Ordres Seed'}
      </button>

      {message && (
        <div style={{
          marginTop: '0.5rem',
          padding: '0.5rem',
          borderRadius: '3px',
          backgroundColor: message.includes('✅') ? '#d4edda' :
                          message.includes('⚠️') ? '#fff3cd' : '#f8d7da',
          color: message.includes('✅') ? '#155724' :
                 message.includes('⚠️') ? '#856404' : '#721c24',
          fontSize: '0.9em'
        }}>
          {message}
        </div>
      )}
    </div>
  );
}

export default QuickOrders;
