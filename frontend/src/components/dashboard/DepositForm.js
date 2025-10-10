import React, { useState, useEffect } from 'react';

function DepositForm({ onDeposit }) {
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      const token = localStorage.getItem('jwt');
      console.log('JWT Token:', token); // Debug log
      const userId = localStorage.getItem('userId');
      console.log('User ID:', userId); // Debug log
      if (!userId) {
        setError("Impossible de trouver l'identifiant utilisateur.");
        setLoading(false);
        return;
      }
      const response = await fetch(`${process.env.REACT_APP_API_URL}/wallet/deposit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId,
          'Idempotency-Key': crypto.randomUUID(),
        },
        body: JSON.stringify({ amount: parseFloat(amount) })
      });
      if (response.status === 401) {
        alert("Session expirée. Veuillez vous reconnecter.");
        localStorage.removeItem('jwt');
        localStorage.removeItem('userId');
        window.location.href = '/login';
        return;
      }
      const data = await response.json();
      if (response.ok && data.success) {
        setSuccess(`Dépôt réussi ! Nouveau solde : ${data.newBalance}`);
        setAmount('');
        if (onDeposit) onDeposit(data);
      } else {
        setError(data.message || `Erreur (${data.status}) : ${data.error}` || 'Erreur lors du dépôt.');
      }
    } catch (err) {
      setError('Erreur réseau ou serveur');
    }
    setLoading(false);
  };

  return (
    <form onSubmit={handleSubmit} style={{ margin: '2rem 0' }}>
      <h3>Déposer de l'argent</h3>
      <input
        type="number"
        min="0.01"
        step="0.01"
        value={amount}
        onChange={e => setAmount(e.target.value)}
        placeholder="Montant à déposer"
        required
        disabled={loading}
        style={{ marginRight: '1rem' }}
      />
      <button type="submit" disabled={loading || !amount}>Déposer</button>
      {error && <div style={{ color: 'red', marginTop: '1rem' }}>{error}</div>}
      {success && <div style={{ color: 'green', marginTop: '1rem' }}>{success}</div>}
    </form>
  );
}

export default DepositForm;
