import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function createStompClient({ token, onNotification, onApplicationUpdate, companyId }) {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    onConnect: () => {
      if (onNotification) {
        client.subscribe('/user/queue/notifications', (msg) => {
          try { onNotification(JSON.parse(msg.body)); } catch { /* silent */ }
        });
      }
      if (onApplicationUpdate && companyId) {
        client.subscribe(`/topic/company/${companyId}/applications`, (msg) => {
          try { onApplicationUpdate(JSON.parse(msg.body)); } catch { /* silent */ }
        });
      }
    },
    onStompError: () => { /* silent — reconnect automatique */ },
  });

  return client;
}
