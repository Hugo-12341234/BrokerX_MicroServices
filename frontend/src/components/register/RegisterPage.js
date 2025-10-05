import React, { useState } from 'react';
import './RegisterPage.css';

function RegisterPage() {
  const [form, setForm] = useState({
    email: '',
    password: '',
    name: '',
    adresse: '',
    dateDeNaissance: ''
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/users/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(form)
      });
      if (response.ok) {
        // Succès, tu peux rediriger ou afficher un message
        alert('Inscription réussie !');
      } else {
        // Erreur, affiche le message d'erreur
        const error = await response.json();
        alert(error.message || 'Erreur lors de l\'inscription');
      }
    } catch (err) {
      alert('Erreur réseau ou serveur');
    }
  };

  return (
    <div className="register-container">
      <h2>Inscription à BrokerX</h2>
      <form className="register-form" onSubmit={handleSubmit}>
        <label>Email :</label>
        <input type="email" name="email" value={form.email} onChange={handleChange} required />

        <label>Mot de passe :</label>
        <input type="password" name="password" value={form.password} onChange={handleChange} required />

        <label>Nom :</label>
        <input type="text" name="name" value={form.name} onChange={handleChange} required />

        <label>Adresse :</label>
        <input type="text" name="adresse" value={form.adresse} onChange={handleChange} required />

        <label>Date de naissance :</label>
        <input type="date" name="dateDeNaissance" value={form.dateDeNaissance} onChange={handleChange} required />

        <button type="submit">S'inscrire</button>
      </form>
    </div>
  );
}

export default RegisterPage;
