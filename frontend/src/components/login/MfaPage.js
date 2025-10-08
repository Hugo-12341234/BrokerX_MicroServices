import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './MfaPage.css';

function MfaPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { challengeId } = location.state || {};
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [locked, setLocked] = useState(false);
  const [lockTimer, setLockTimer] = useState(0);

  const handleChange = (e) => {
    setCode(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/auth/verify-mfa`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ challengeId, code })
      });
      const data = await response.json();
      if (response.ok && data.success && data.status === 'success') {
        if (data.token) {
          localStorage.setItem('jwt', data.token);
        }
        if (data.userId) {
            localStorage.setItem('userId', data.userId);
        }
        alert(data.message);
        navigate('/dashboard');
      } else if (data.status === 'locked') {
        setError(data.message);
        setLocked(true);
        // Extraire le temps restant si présent dans le message
        const match = data.message.match(/(\d+) secondes/);
        if (match) {
          setLockTimer(parseInt(match[1], 10));
          let seconds = parseInt(match[1], 10);
          const interval = setInterval(() => {
            seconds--;
            setLockTimer(seconds);
            if (seconds <= 0) {
              setLocked(false);
              setLockTimer(0);
              setError(''); // On retire le message d'erreur et le message de blocage
              clearInterval(interval);
            }
          }, 1000);
        }
      } else if (data.status === 'suspended') {
        setError(data.message);
        alert(data.message);
        setTimeout(() => {
          navigate('/login');
        }, 3500);
      } else {
        setError(data.message || 'Erreur lors de la vérification MFA');
      }
    } catch (err) {
      setError('Erreur réseau ou serveur');
    }
  };

  if (!challengeId) {
    return <div className="login-container"><p>ChallengeId manquant. Veuillez vous reconnecter.</p></div>;
  }

  return (
    <div className="login-container">
      <h2>Vérification MFA</h2>
      <form className="login-form" onSubmit={handleSubmit}>
        <label>Code reçu par courriel :</label>
        <input type="text" name="code" value={code} onChange={handleChange} required disabled={locked} />
        <button type="submit" disabled={locked}>Vérifier</button>
      </form>
      {error && <div className="mfa-error">{error}</div>}
      {locked && lockTimer > 0 && (
        <div className="mfa-locked">Vous êtes temporairement bloqué. Veuillez patienter {lockTimer} secondes.</div>
      )}
    </div>
  );
}

export default MfaPage;
