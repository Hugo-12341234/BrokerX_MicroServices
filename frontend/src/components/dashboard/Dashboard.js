import React, { useEffect, useState } from 'react';
import DepositForm from './DepositForm';
import QuickOrders from './QuickOrders';
import { useNavigate } from 'react-router-dom';
import OrdersDashboard from '../OrdersDashboard';
import '../OrdersDashboard.css';
import NotificationsListener from '../NotificationsListener';

function Dashboard() {
  const [wallet, setWallet] = useState(null);
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notification, setNotification] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchWallet = async () => {
      setLoading(true);
      setError('');
      try {
        const token = localStorage.getItem('jwt');
        const userId = localStorage.getItem('userId');
        const response = await fetch(`${process.env.REACT_APP_API_URL}/wallet`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'X-User-Id': userId
          }
        });
        console.log("jwt:", token); // Debug log
        console.log("userId:", userId); // Debug log
        console.log('Fetch Wallet Response Status:', response.status); // Debug log
        if (response.status === 401) {
            // Token invalide ou expiré, rediriger vers la page de connexion
            alert("Session expirée. Veuillez vous reconnecter.");
            localStorage.removeItem('jwt');
            localStorage.removeItem('userId');
            navigate('/login');
            return;
        }
        const data = await response.json();
        console.log('Fetch Wallet Response Data:', data); // Debug log
        if (response.ok && data.success && data.wallet) {
          setWallet(data.wallet);
          setBalance(data.wallet.balance);
        } else {
          setError(data.message || `Erreur (${data.status}) : ${data.error}` || 'Erreur lors de la récupération du portefeuille');
        }
      } catch (err) {
        setError('Erreur réseau ou serveur');
      }
      setLoading(false);
    };
    fetchWallet();
  }, [navigate]);

  const handleDeposit = (depositResult) => {
    if (depositResult && depositResult.newBalance !== undefined) {
      setBalance(depositResult.newBalance);
      if (wallet) {
        setWallet({ ...wallet, balance: depositResult.newBalance });
      }
    }
  };

  return (
    <div style={{ maxWidth: 500, margin: '0 auto', padding: '2rem' }}>
      <NotificationsListener userId={localStorage.getItem('userId')} onNotification={msg => setNotification(msg)} />
      {notification && (
        <div style={{ background: '#ffeeba', color: '#856404', padding: '10px', borderRadius: '5px', marginBottom: '1rem', fontWeight: 'bold' }}>
          Notification : {notification}
        </div>
      )}
      <h2>Tableau de bord du portefeuille</h2>
      {loading ? (
        <div>Chargement du solde...</div>
      ) : error ? (
        <div style={{ color: 'red' }}>{error}</div>
      ) : wallet ? (
        <div style={{ marginBottom: '2rem' }}>
          <strong>Solde actuel :</strong> {wallet.balance} $
          <br />
          <strong>Date de création :</strong> {wallet.createdAt ? new Date(wallet.createdAt).toLocaleString() : 'N/A'}
        </div>
      ) : null}
      <button onClick={() => navigate('/order')} style={{ marginBottom: '1rem' }}>
        Placer un ordre
      </button>
      <DepositForm onDeposit={handleDeposit} />
      <QuickOrders wallet={wallet} />
      {wallet && wallet.stockPositions && wallet.stockPositions.length > 0 && (
        <div style={{ marginTop: 30 }}>
          <h3>Vos positions en actions</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={{ borderBottom: '1px solid #ccc', textAlign: 'left' }}>Symbole</th>
                <th style={{ borderBottom: '1px solid #ccc', textAlign: 'right' }}>Quantité</th>
              </tr>
            </thead>
            <tbody>
              {wallet.stockPositions.map(pos => (
                <tr key={pos.id}>
                  <td>{pos.symbol}</td>
                  <td style={{ textAlign: 'right' }}>{pos.quantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div style={{ marginTop: 40 }}>
        <OrdersDashboard userId={localStorage.getItem('userId')} />
      </div>
    </div>
  );
}

export default Dashboard;
