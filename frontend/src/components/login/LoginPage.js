import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';

function LoginPage() {
  const [form, setForm] = useState({
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(form)
      });
      const data = await response.json();
      if (response.ok) {
        // Affiche le message de succès ou gère la MFA
        alert(data.message);
        if (data.requiresMfa) {
          // Ici tu pourras rediriger vers la page MFA ou gérer le challengeId
          navigate('/mfa', { state: { challengeId: data.challengeId } });
        }
      } else {
        setError(data.message || `Erreur (${data.status}) : ${data.error}` || 'Erreur lors de la connexion');
      }
    } catch (err) {
      setError('Erreur réseau ou serveur');
    }
  };

  return (
    <div className="login-container">
      <h2>Connexion à BrokerX</h2>
      <form className="login-form" onSubmit={handleSubmit}>
        <label>Email :</label>
        <input type="email" name="email" value={form.email} onChange={handleChange} required />

        <label>Mot de passe :</label>
        <input type="password" name="password" value={form.password} onChange={handleChange} required />

        <button type="submit">Se connecter</button>
      </form>
      {error && <div style={{ color: 'red', marginTop: '1rem' }}>{error}</div>}
      <div className="login-register-link">
        <span>Pas de compte ? </span>
        <a href="/register">Inscrivez-vous</a>
      </div>
    </div>
  );
}

export default LoginPage;
