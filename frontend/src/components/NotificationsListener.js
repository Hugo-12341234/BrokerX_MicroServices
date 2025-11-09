import React, { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const NotificationsListener = ({ userId, onNotification }) => {
  const stompClientRef = useRef(null);

  useEffect(() => {
    if (!userId) return;
    const socket = new SockJS(`${process.env.REACT_APP_API_URL.replace('/api/v1', '')}/ws`);
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient.subscribe(`/topic/notifications/${userId}`, (message) => {
          if (onNotification) onNotification(message.body);
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      }
    });
    stompClient.activate();
    stompClientRef.current = stompClient;
    return () => {
      stompClientRef.current && stompClientRef.current.deactivate();
    };
  }, [userId, onNotification]);

  return null;
};

export default NotificationsListener;

