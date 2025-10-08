import React, { useEffect, useState } from 'react';
import DepositForm from './DepositForm';
import { useNavigate } from 'react-router-dom';

function Dashboard() {
  const [wallet, setWallet] = useState(null);
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
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
        if (response.ok && data.success && data.wallet) {
          setWallet(data.wallet);
          setBalance(data.wallet.balance);
        } else {
          setError(data.message || 'Erreur lors de la récupération du portefeuille.');
        }
      } catch (err) {
        setError('Erreur réseau ou serveur.');
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
      <DepositForm onDeposit={handleDeposit} />
    </div>
  );
}

export default Dashboard;
