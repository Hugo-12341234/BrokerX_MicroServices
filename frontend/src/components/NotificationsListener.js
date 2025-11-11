import React, { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const NotificationsListener = ({ userId, onNotification }) => {
  const stompClientRef = useRef(null);

  useEffect(() => {
    if (!userId) return;
    // Construction de l'URL WebSocket
    const wsBaseUrl = process.env.REACT_APP_API_URL.replace('/api/v1', '');
    const wsUrl = `${wsBaseUrl}/ws`;
    console.log('[NotificationsListener] WebSocket URL utilisé :', wsUrl);
    const socket = new SockJS(wsUrl);
    const stompClient = new Client();
    stompClient.webSocketFactory = () => socket;
    stompClient.reconnectDelay = 5000;
    stompClient.onConnect = () => {
      console.log('[NotificationsListener] Connecté au WebSocket');
      stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
        console.log('[NotificationsListener] Message reçu :', message.body);
        if (onNotification) onNotification(message.body);
      });
    };
    stompClient.onStompError = (frame) => {
      console.error('[NotificationsListener] Erreur STOMP :', frame);
    };
    stompClient.onWebSocketClose = (event) => {
      console.warn('[NotificationsListener] WebSocket fermé :', event);
    };
    stompClient.onWebSocketError = (event) => {
      console.error('[NotificationsListener] Erreur WebSocket :', event);
    };
    stompClient.activate();
    stompClientRef.current = stompClient;
    return () => {
      console.log('[NotificationsListener] Déconnexion du WebSocket');
      stompClientRef.current && stompClientRef.current.deactivate();
    };
  }, [userId, onNotification]);

  return null;
};

export default NotificationsListener;
