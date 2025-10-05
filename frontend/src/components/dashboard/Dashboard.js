import React from 'react';
import './Dashboard.css';

function Dashboard() {
  return (
    <div className="dashboard-container">
      <aside className="dashboard-sidebar">
        <h2>BrokerX</h2>
        <nav>
          <ul>
            <li>Accueil</li>
            <li>Portefeuille</li>
            <li>Transactions</li>
            <li>Profil</li>
            <li>Déconnexion</li>
          </ul>
        </nav>
      </aside>
      <main className="dashboard-main">
        <header className="dashboard-header">
          <h1>Bienvenue sur BrokerX</h1>
        </header>
        <section className="dashboard-content">
          <p>Votre tableau de bord personnalisé apparaîtra ici.</p>
        </section>
      </main>
    </div>
  );
}

export default Dashboard;

