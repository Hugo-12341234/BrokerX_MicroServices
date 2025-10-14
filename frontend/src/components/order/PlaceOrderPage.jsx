import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';

const SYMBOLS = ['AAPL', 'MSFT', 'TSLA', 'GOOG'];
const SIDES = [
  { value: 'ACHAT', label: 'Achat' },
  { value: 'VENTE', label: 'Vente' }
];
const TYPES = [
  { value: 'MARCHE', label: 'Marché' },
  { value: 'LIMITE', label: 'Limite' }
];
const DURATIONS = [
  { value: 'DAY', label: 'Jour' },
  { value: 'IOC', label: 'IOC' },
  { value: 'FOK', label: 'FOK' }
];

function PlaceOrderPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    symbol: SYMBOLS[0],
    side: SIDES[0].value,
    type: TYPES[0].value,
    quantity: 1,
    price: '',
    duration: DURATIONS[0].value
  });
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleChange = e => {
    const { name, value } = e.target;
    setForm(f => ({ ...f, [name]: value }));
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setError(null);
    setResult(null);
    try {
      const token = localStorage.getItem('jwt');
      const userId = localStorage.getItem('userId');
      const idempotencyKey = uuidv4();
      const res = await fetch(`${process.env.REACT_APP_API_URL}/orders/place`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId,
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify({
          symbol: form.symbol,
          side: form.side,
          type: form.type,
          quantity: Number(form.quantity),
          price: form.type === 'LIMITE' ? Number(form.price) : null,
          duration: form.duration
        })
      });
      const data = await res.json();
      if (res.ok) setResult(data);
      else setError(data.message || 'Erreur lors du placement de l’ordre');
    } catch (err) {
      setError('Erreur réseau ou serveur');
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: 'auto' }}>
      <button onClick={() => navigate('/dashboard')} style={{ marginBottom: '1rem' }}>Retour au dashboard</button>
      <h2>Placer un ordre</h2>
      <form onSubmit={handleSubmit}>
        <label>Symbole&nbsp;
          <select name="symbol" value={form.symbol} onChange={handleChange}>
            {SYMBOLS.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </label><br /><br />
        <label>Type d’ordre&nbsp;
          <select name="type" value={form.type} onChange={handleChange}>
            {TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </label><br /><br />
        <label>Côté&nbsp;
          <select name="side" value={form.side} onChange={handleChange}>
            {SIDES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </label><br /><br />
        <label>Quantité&nbsp;
          <input type="number" name="quantity" min="1" value={form.quantity} onChange={handleChange} required />
        </label><br /><br />
        {form.type === 'LIMITE' && (
          <label>Prix limite&nbsp;
            <input type="number" name="price" step="0.01" value={form.price} onChange={handleChange} required />
          </label>
        )}<br />
        <label>Durée&nbsp;
          <select name="duration" value={form.duration} onChange={handleChange}>
            {DURATIONS.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
          </select>
        </label><br /><br />
        <button type="submit">Placer l’ordre</button>
      </form>
      {result && <div style={{ color: 'green', marginTop: 20 }}>
        <b>Ordre accepté !</b><br />
        ID: {result.id}<br />
        Statut: {result.status}<br />
        {result.message && <div style={{ marginTop: 10 }}><b>Détails:</b><br />{result.message}</div>}
      </div>}
      {error && <div style={{ color: 'red', marginTop: 20 }}>{error}</div>}
    </div>
  );
}

export default PlaceOrderPage;
