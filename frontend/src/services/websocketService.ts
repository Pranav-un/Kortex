import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_BASE_URL, TOKEN_KEY } from '../config/constants';
import { NotificationMessage } from '../types';

type NotificationCallback = (notification: NotificationMessage) => void;

class WebSocketService {
  private client: Client | null = null;
  private callbacks: NotificationCallback[] = [];
  private isConnecting: boolean = false;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.active) {
        resolve();
        return;
      }

      if (this.isConnecting) {
        reject(new Error('Connection already in progress'));
        return;
      }

      const token = localStorage.getItem(TOKEN_KEY);
      if (!token) {
        reject(new Error('No authentication token found'));
        return;
      }

      this.isConnecting = true;

      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_BASE_URL),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
          console.log('[WebSocket]', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('[WebSocket] Connected');
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          this.subscribe();
          resolve();
        },
        onStompError: (frame) => {
          console.error('[WebSocket] STOMP error:', frame);
          this.isConnecting = false;
          reject(new Error(frame.headers['message'] || 'WebSocket connection failed'));
        },
        onWebSocketError: (event) => {
          console.error('[WebSocket] Error:', event);
          this.isConnecting = false;
          reject(new Error('WebSocket connection error'));
        },
        onDisconnect: () => {
          console.log('[WebSocket] Disconnected');
          this.isConnecting = false;
          this.handleReconnect();
        },
      });

      this.client.activate();
    });
  }

  private subscribe(): void {
    if (!this.client?.active) return;

    // Subscribe to user-specific notifications
    this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
      try {
        const notification: NotificationMessage = JSON.parse(message.body);
        this.notifyCallbacks(notification);
      } catch (error) {
        console.error('[WebSocket] Failed to parse notification:', error);
      }
    });

    // Subscribe to broadcast notifications
    this.client.subscribe('/topic/notifications', (message: IMessage) => {
      try {
        const notification: NotificationMessage = JSON.parse(message.body);
        this.notifyCallbacks(notification);
      } catch (error) {
        console.error('[WebSocket] Failed to parse notification:', error);
      }
    });

    console.log('[WebSocket] Subscribed to notification channels');
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`[WebSocket] Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => {
        this.connect().catch((error) => {
          console.error('[WebSocket] Reconnect failed:', error);
        });
      }, 5000 * this.reconnectAttempts);
    } else {
      console.error('[WebSocket] Max reconnect attempts reached');
    }
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.callbacks = [];
      this.reconnectAttempts = 0;
      console.log('[WebSocket] Disconnected and cleaned up');
    }
  }

  onNotification(callback: NotificationCallback): () => void {
    this.callbacks.push(callback);

    // Return unsubscribe function
    return () => {
      this.callbacks = this.callbacks.filter((cb) => cb !== callback);
    };
  }

  private notifyCallbacks(notification: NotificationMessage): void {
    this.callbacks.forEach((callback) => {
      try {
        callback(notification);
      } catch (error) {
        console.error('[WebSocket] Callback error:', error);
      }
    });
  }

  isConnected(): boolean {
    return this.client?.active || false;
  }
}

export const websocketService = new WebSocketService();
