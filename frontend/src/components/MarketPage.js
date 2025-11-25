import React, { useState, useEffect } from 'react';
import OrderBookView from './OrderBookView';
import SockJS from 'sockjs-client';
import { useNavigate } from 'react-router-dom';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8079/api/v1';
const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8079/ws/market-data';

function MarketPage() {
  const [symbols, setSymbols] = useState([]);
  const [subscribedSymbols, setSubscribedSymbols] = useState([]); // Liste des symboles abonnés
  const [orderBooks, setOrderBooks] = useState({}); // {symbol: orderBook[]}
  const [lastPrices, setLastPrices] = useState({}); // {symbol: lastPrice}
  const wsClientsRef = React.useRef({}); // {symbol: wsClient}
  const navigate = useNavigate();

  useEffect(() => {
    const fetchSymbols = async () => {
      const token = localStorage.getItem('jwt');
      const userId = localStorage.getItem('userId');
      try {
        const response = await fetch(`${API_URL}/market-data/symbols`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'X-User-Id': userId
          }
        });
        const data = await response.json();
        setSymbols(data.symbols || []);
      } catch (err) {
        setSymbols([]);
      }
    };
    fetchSymbols();
    // Récupère les abonnements persistés
    const saved = localStorage.getItem('subscribedSymbols');
    if (saved) {
      try {
        const arr = JSON.parse(saved);
        if (Array.isArray(arr)) {
          setSubscribedSymbols(arr);
        }
      } catch {}
    }
  }, []);

  // Sauvegarde les abonnements à chaque changement
  useEffect(() => {
    localStorage.setItem('subscribedSymbols', JSON.stringify(subscribedSymbols));
  }, [subscribedSymbols]);

  // Abonnement à un symbole
  const subscribeToSymbol = (symbol) => {
    if (subscribedSymbols.includes(symbol)) return;
    setSubscribedSymbols(prev => [...prev, symbol]);
    const token = localStorage.getItem('jwt');
    const userId = localStorage.getItem('userId');
    // Fetch initial orderbook
    fetch(`${API_URL}/market-data/orderbook?symbol=${symbol}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    })
      .then(res => res.json())
      .then(data => setOrderBooks(prev => ({ ...prev, [symbol]: data.orders ? data.orders.slice(0, 10) : [] })))
      .catch(() => setOrderBooks(prev => ({ ...prev, [symbol]: [] })));
    // Fetch initial last price
    fetch(`${API_URL}/market-data/last-price?symbol=${symbol}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    })
      .then(res => res.json())
      .then(data => {
        setLastPrices(prev => ({ ...prev, [symbol]: data.lastPrice || null }));
        console.log(`[MarketPage] GET last-price appelé pour ${symbol}, valeur reçue:`, data.lastPrice);
      })
      .catch(() => {
        setLastPrices(prev => ({ ...prev, [symbol]: null }));
        console.log(`[MarketPage] GET last-price erreur pour ${symbol}`);
      });
    // Setup SockJS (raw)
    const ws = new SockJS(`${WS_URL}`); // Connexion à l'endpoint unique
    ws.onopen = () => {
      console.log(`[MarketPage] SockJS ouvert pour ${symbol}`);
      // Optionnel : envoyer un message d'abonnement au symbol si le backend le requiert
      // ws.send(JSON.stringify({ action: 'subscribe', symbol }));
    };
    ws.onmessage = (event) => {
      console.log(`[MarketPage] Message reçu du WebSocket pour ${symbol}:`, event.data);
      try {
        const data = JSON.parse(event.data);
        // Filtrer les messages pour le symbol courant
        if (data.symbol === symbol) {
          if (data.orderBook) setOrderBooks(prev => ({ ...prev, [symbol]: data.orderBook.slice(0, 10) }));
          if (data.lastPrice !== undefined) setLastPrices(prev => ({ ...prev, [symbol]: data.lastPrice }));
        }
      } catch (e) {
        console.error(`[MarketPage] Erreur parsing SockJS pour ${symbol}:`, e);
      }
    };
    ws.onclose = (event) => {
      console.log(`[MarketPage] SockJS fermé pour ${symbol}:`, event);
    };
    ws.onerror = (event) => {
      console.error(`[MarketPage] SockJS erreur pour ${symbol}:`, event);
    };
    wsClientsRef.current[symbol] = ws;
  };

  // Réabonne automatiquement aux symboles persistés
  useEffect(() => {
    subscribedSymbols.forEach(symbol => {
      if (!wsClientsRef.current[symbol]) {
        const token = localStorage.getItem('jwt');
        const userId = localStorage.getItem('userId');
        // Fetch initial orderbook
        fetch(`${API_URL}/market-data/orderbook?symbol=${symbol}`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'X-User-Id': userId
          }
        })
          .then(res => res.json())
          .then(data => setOrderBooks(prev => ({ ...prev, [symbol]: data.orders ? data.orders.slice(0, 10) : [] })))
          .catch(() => setOrderBooks(prev => ({ ...prev, [symbol]: [] })));
        // Fetch initial last price
        fetch(`${API_URL}/market-data/last-price?symbol=${symbol}`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'X-User-Id': userId
          }
        })
          .then(res => res.json())
          .then(data => setLastPrices(prev => ({ ...prev, [symbol]: data.lastPrice || null })))
          .catch(() => setLastPrices(prev => ({ ...prev, [symbol]: null })));
        // Setup SockJS (raw)
        const ws = new SockJS(`${WS_URL}`); // Connexion à l'endpoint unique
        ws.onopen = () => {
          console.log(`[MarketPage] SockJS ouvert pour ${symbol}`);
          // Optionnel : envoyer un message d'abonnement au symbol si le backend le requiert
          // ws.send(JSON.stringify({ action: 'subscribe', symbol }));
        };
        ws.onmessage = (event) => {
          console.log(`[MarketPage] Message reçu du WebSocket pour ${symbol}:`, event.data);
          try {
            const data = JSON.parse(event.data);
            // Filtrer les messages pour le symbol courant
            if (data.symbol === symbol) {
              if (data.orderBook) setOrderBooks(prev => ({ ...prev, [symbol]: data.orderBook.slice(0, 10) }));
              if (data.lastPrice !== undefined) setLastPrices(prev => ({ ...prev, [symbol]: data.lastPrice }));
            }
          } catch (e) {
            console.error(`[MarketPage] Erreur parsing SockJS pour ${symbol}:`, e);
          }
        };
        ws.onclose = (event) => {
          console.log(`[MarketPage] SockJS fermé pour ${symbol}:`, event);
        };
        ws.onerror = (event) => {
          console.error(`[MarketPage] SockJS erreur pour ${symbol}:`, event);
        };
        wsClientsRef.current[symbol] = ws;
      }
    });
  }, [subscribedSymbols]);

  // Cleanup des websockets à l'unmount
  useEffect(() => {
    return () => {
      Object.values(wsClientsRef.current).forEach(ws => {
        if (ws && ws.readyState === 1) ws.close();
      });
    };
  }, []);

  return (
    <div>
        <button onClick={() => navigate('/dashboard')}>Retour au dashboard</button>
        <h2>Marché</h2>
      <div>
        <h3>Symboles disponibles :</h3>
        {symbols.map(symbol => (
          <div key={symbol} style={{ display: 'inline-block', marginRight: '10px' }}>
            <button onClick={() => subscribeToSymbol(symbol)} disabled={subscribedSymbols.includes(symbol)}>
              S'abonner à {symbol}
            </button>
            {subscribedSymbols.includes(symbol) && (
              <button onClick={() => {
                setSubscribedSymbols(prev => prev.filter(s => s !== symbol));
                // Cleanup WebSocket
                if (wsClientsRef.current[symbol]) {
                  wsClientsRef.current[symbol].close();
                  delete wsClientsRef.current[symbol];
                }
                // Cleanup affichage
                setOrderBooks(prev => {
                  const copy = { ...prev };
                  delete copy[symbol];
                  return copy;
                });
                setLastPrices(prev => {
                  const copy = { ...prev };
                  delete copy[symbol];
                  return copy;
                });
              }} style={{ marginLeft: '5px' }}>
                Se désabonner
              </button>
            )}
          </div>
        ))}
      </div>
      {subscribedSymbols.map(symbol => (
        <OrderBookView key={symbol} symbol={symbol} orderBook={orderBooks[symbol]} lastPrice={lastPrices[symbol]} />
      ))}
    </div>
  );
}

export default MarketPage;
