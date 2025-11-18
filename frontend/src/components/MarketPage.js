import React, { useState, useEffect } from 'react';
import OrderBookView from './OrderBookView';
import SockJS from 'sockjs-client';
import { over } from 'stompjs';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8079/api/v1/market-data';
const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8079/ws/market-data';

function MarketPage() {
  const [symbols, setSymbols] = useState([]);
  const [subscribedSymbols, setSubscribedSymbols] = useState([]); // Liste des symboles abonnés
  const [orderBooks, setOrderBooks] = useState({}); // {symbol: orderBook[]}
  const [lastPrices, setLastPrices] = useState({}); // {symbol: lastPrice}
  const stompClientsRef = React.useRef({}); // {symbol: stompClient}

  useEffect(() => {
    // Fetch all symbols au chargement
    fetch(`${API_URL}/symbols`)
      .then(res => res.json())
      .then(data => setSymbols(data.symbols || []))
      .catch(() => setSymbols([]));
  }, []);

  // Abonnement à un symbole
  const subscribeToSymbol = (symbol) => {
    if (subscribedSymbols.includes(symbol)) return;
    setSubscribedSymbols(prev => [...prev, symbol]);
    // Fetch initial orderbook
    fetch(`${API_URL}/orderbook?symbol=${symbol}`)
      .then(res => res.json())
      .then(data => setOrderBooks(prev => ({ ...prev, [symbol]: data.orders ? data.orders.slice(-10) : [] })))
      .catch(() => setOrderBooks(prev => ({ ...prev, [symbol]: [] })));
    // Fetch initial last price
    fetch(`${API_URL}/last-price?symbol=${symbol}`)
      .then(res => res.json())
      .then(data => setLastPrices(prev => ({ ...prev, [symbol]: data.lastPrice || null })))
      .catch(() => setLastPrices(prev => ({ ...prev, [symbol]: null })));
    // Setup WebSocket
    const sock = new SockJS(WS_URL);
    const client = over(sock);
    client.connect({}, () => {
      client.subscribe(`/topic/market-data/${symbol}`, msg => {
        const data = JSON.parse(msg.body);
        if (data.orderBook) setOrderBooks(prev => ({ ...prev, [symbol]: data.orderBook.slice(-10) }));
        if (data.lastPrice !== undefined) setLastPrices(prev => ({ ...prev, [symbol]: data.lastPrice }));
      });
    });
    stompClientsRef.current[symbol] = client;
  };

  // Cleanup des websockets à l'unmount
  useEffect(() => {
    return () => {
      Object.values(stompClientsRef.current).forEach(client => {
        if (client) client.disconnect();
      });
    };
  }, []);

  return (
    <div>
      <h2>Marché</h2>
      <div>
        <h3>Symboles disponibles :</h3>
        {symbols.map(symbol => (
          <button key={symbol} onClick={() => subscribeToSymbol(symbol)} disabled={subscribedSymbols.includes(symbol)}>
            S'abonner à {symbol}
          </button>
        ))}
      </div>
      {subscribedSymbols.map(symbol => (
        <OrderBookView key={symbol} symbol={symbol} orderBook={orderBooks[symbol]} lastPrice={lastPrices[symbol]} />
      ))}
    </div>
  );
}

export default MarketPage;
