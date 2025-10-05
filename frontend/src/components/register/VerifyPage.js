import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';

function VerifyPage() {
  const [searchParams] = useSearchParams();
  const [message, setMessage] = useState('');
  const token = searchParams.get('token');

  useEffect(() => {
    if (token) {
      fetch(`${process.env.REACT_APP_API_URL}/verify?token=${token}`)
        .then(res => res.json())
        .then(data => setMessage(data.message || 'Erreur inconnue'))
        .catch(() => setMessage('Erreur réseau ou serveur'));
    } else {
      setMessage('Token manquant dans l’URL');
    }
  }, [token]);

  return (
    <div className="register-container">
      <h2>Activation du compte</h2>
      <p>{message}</p>
      {message === "Votre compte a été activé avec succès !" && (
        <Link to="/login">Aller à la page de connexion</Link>
      )}
    </div>
  );
}

export default VerifyPage;
