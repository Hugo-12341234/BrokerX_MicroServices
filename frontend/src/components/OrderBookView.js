import React from 'react';

function OrderBookView({ symbol, orderBook, lastPrice }) {
  return (
    <div>
      <h3>OrderBook pour {symbol}</h3>
      <table>
        <thead>
          <tr>
            <th>Type</th>
            <th>Quantité</th>
            <th>Prix</th>
            <th>Statut</th>
            <th>Timestamp</th>
          </tr>
        </thead>
        <tbody>
          {orderBook && orderBook.length > 0 ? orderBook.map((order, idx) => (
            <tr key={idx}>
              <td>{order.side}</td>
              <td>{order.quantity}</td>
              <td>{order.price}</td>
              <td>{order.status}</td>
              <td>{order.timestamp}</td>
            </tr>
          )) : <tr><td colSpan={5}>Aucune donnée</td></tr>}
        </tbody>
      </table>
      <div style={{marginTop: '1em'}}>
        <strong>Dernier prix exécuté :</strong> {lastPrice !== null ? lastPrice : 'Aucune exécution'}
      </div>
    </div>
  );
}

export default OrderBookView;

