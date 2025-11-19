import React from 'react';

function OrderBookView({ symbol, orderBook = [], lastPrice }) {
  return (
    <div style={{ margin: '20px 0', border: '1px solid #ccc', padding: '10px' }}>
      <h4>OrderBook pour {symbol}</h4>
      <div>Dernier prix exécuté : <b>{lastPrice !== undefined && lastPrice !== null ? lastPrice : 'N/A'}</b></div>
      <table style={{ width: '100%', marginTop: '10px', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Quantité totale</th>
            <th>Quantité restante</th>
            <th>Prix</th>
            <th>Statut</th>
            <th>User</th>
          </tr>
        </thead>
        <tbody>
          {orderBook.length === 0 ? (
            <tr><td colSpan={7}>Aucun ordre</td></tr>
          ) : (
            orderBook.map(order => (
              <tr key={order.id}>
                <td>{order.id}</td>
                <td>{order.side}</td>
                <td>{order.quantity}</td>
                <td>{order.quantityRemaining !== undefined ? order.quantityRemaining : (order.quantity - (order.filledQuantity || 0))}</td>
                <td>{order.price}</td>
                <td>{order.status}</td>
                <td>{order.userId}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export default OrderBookView;
