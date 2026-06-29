import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { ChatMessage } from '../models/chat-message.model';

@Injectable({ providedIn: 'root' })
export class ChatService {

  private readonly apiUrl = 'http://localhost:8080/api/chat';
  private readonly wsUrl  = 'ws://localhost:8080/ws-raw';

  private stompClient!: Client;
  private messageSubject = new Subject<ChatMessage>();

  messages$ = this.messageSubject.asObservable();

  constructor(private http: HttpClient) {}

  connect(onConnected: () => void): void {
    this.stompClient = new Client({
      brokerURL: this.wsUrl,
      onConnect: () => {
        this.stompClient.subscribe('/topic/messages', (frame: IMessage) => {
          const message: ChatMessage = JSON.parse(frame.body);
          this.messageSubject.next(message);
        });
        onConnected();
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      }
    });
    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient?.active) {
      this.stompClient.deactivate();
    }
  }

  sendMessage(username: string, content: string) {
    return this.http.post<void>(`${this.apiUrl}/send`, { username, content });
  }

  getHistory() {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/history`);
  }
}
