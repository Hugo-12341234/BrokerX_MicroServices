import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import EditOrderModal from './EditOrderModal';

const OrdersDashboard = ({ userId }) => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const location = useLocation();

  // Définir fetchOrdersAsync en dehors du useEffect pour qu'il soit accessible partout
  const fetchOrdersAsync = async () => {
    setLoading(true);
    setError(null);
    try {
      console.log("Fetching orders for userId:", userId); // Debug log
      const token = localStorage.getItem('jwt');
      const res = await fetch(`${process.env.REACT_APP_API_URL}/orders`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        }
      });
      if (!res.ok) throw new Error('Erreur lors de la récupération des ordres');
      const data = await res.json();
      setOrders(data);
      data[0] && console.log("First order fetched:", data[0]); // Debug log
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    console.log("Use effect");
    fetchOrdersAsync();
  }, [userId, location.pathname]);

  const handleEdit = (order) => {
    setSelectedOrder(order);
    setShowEditModal(true);
  };

  const handleCancel = async (orderId) => {
    if (!window.confirm('Confirmer l\'annulation de cet ordre ?')) return;
    try {
        const token = localStorage.getItem('jwt');
        const res = await fetch(`/api/v1/orders/${orderId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
            'X-User-Id': userId
        }
      });
      if (!res.ok) throw new Error('Erreur lors de l\'annulation');
      fetchOrdersAsync();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleEditSubmit = async (orderId, updatedOrder) => {
    try {
        const token = localStorage.getItem('jwt');
        const res = await fetch(`/api/v1/orders/${orderId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        },
        body: JSON.stringify(updatedOrder)
      });
      if (!res.ok) throw new Error('Erreur lors de la modification');
      setShowEditModal(false);
      setSelectedOrder(null);
      fetchOrdersAsync();
    } catch (err) {
      alert(err.message);
    }
  };

  if (loading) return <div>Chargement des ordres...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <div>
      <h2>Mes ordres</h2>
      {orders.length === 0 ? (
        <div>Aucun ordre trouvé.</div>
      ) : (
        <table className="orders-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Symbole</th>
              <th>Quantité</th>
              <th>Prix</th>
              <th>Type</th>
              <th>Side</th>
              <th>Statut</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {[...orders].reverse().map(order => (
              <tr key={order.id}>
                <td>{order.id}</td>
                <td>{order.symbol}</td>
                <td>{order.quantity}</td>
                <td>{order.price}</td>
                <td>{order.type}</td>
                <td>{order.side}</td>
                <td>{order.status}</td>
                <td>{order.timestamp}</td>
                <td>
                  <button onClick={() => handleEdit(order)}>Modifier</button>
                  <button onClick={() => handleCancel(order.id)} style={{ marginLeft: 8 }}>Annuler</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {showEditModal && (
        <EditOrderModal
          order={selectedOrder}
          onClose={() => setShowEditModal(false)}
          onSubmit={handleEditSubmit}
        />
      )}
    </div>
  );
};

export default OrdersDashboard;
