import React, { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';

const SYMBOLS = ['AAPL', 'MSFT', 'TSLA'];
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

const EditOrderModal = ({ order, onClose, onSubmit }) => {
  const [form, setForm] = useState({
    symbol: order.symbol,
    side: order.side,
    type: order.type,
    quantity: order.quantity,
    price: order.price || '',
    duration: order.duration
  });
  const [error, setError] = useState(null);

  const handleChange = e => {
    const { name, value } = e.target;
    setForm(f => ({ ...f, [name]: value }));
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setError(null);
    try {
      // OnSubmit est passé par le parent, il fait l'appel API et gère le refresh
      await onSubmit(order.id, {
        symbol: form.symbol,
        side: form.side,
        type: form.type,
        quantity: Number(form.quantity),
        price: form.type === 'LIMITE' ? Number(form.price) : null,
        duration: form.duration
      });
    } catch (err) {
      setError('Erreur lors de la modification');
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h3>Modifier l'ordre #{order.id}</h3>
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
          <button type="submit">Enregistrer</button>
          <button type="button" onClick={onClose} style={{ marginLeft: 8 }}>Annuler</button>
        </form>
        {error && <div style={{ color: 'red', marginTop: 20 }}>{error}</div>}
      </div>
    </div>
  );
};

export default EditOrderModal;
