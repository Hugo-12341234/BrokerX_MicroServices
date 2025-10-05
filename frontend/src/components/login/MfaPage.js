import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './MfaPage.css';

function MfaPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { challengeId } = location.state || {};
  const [code, setCode] = useState('');

  const handleChange = (e) => {
    setCode(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/auth/verify-mfa`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ challengeId, code })
      });
      const data = await response.json();
      if (response.ok && data.success) {
        alert(data.message);
        navigate('/dashboard');
      } else {
        alert(data.message || 'Erreur lors de la vérification MFA');
      }
    } catch (err) {
      alert('Erreur réseau ou serveur');
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
        <input type="text" name="code" value={code} onChange={handleChange} required />
        <button type="submit">Vérifier</button>
      </form>
    </div>
  );
}

export default MfaPage;
