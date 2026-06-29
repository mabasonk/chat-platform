import { Component, Input, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../models/chat-message.model';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() username!: string;
  @ViewChild('messageList') messageList!: ElementRef;

  messages: ChatMessage[] = [];
  newMessage = '';
  connected = false;
  private sub!: Subscription;

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {
    this.sub = this.chatService.messages$.subscribe(msg => {
      this.messages.push(msg);
    });

    this.chatService.connect(() => {
      this.connected = true;
      this.chatService.getHistory().subscribe(history => {
        this.messages = [...history, ...this.messages];
      });
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.chatService.disconnect();
  }

  send(): void {
    const content = this.newMessage.trim();
    if (!content || !this.connected) return;

    this.chatService.sendMessage(this.username, content).subscribe();
    this.newMessage = '';
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  isOwnMessage(msg: ChatMessage): boolean {
    return msg.username === this.username;
  }

  formatTime(timestamp: string): string {
    return new Date(timestamp).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private scrollToBottom(): void {
    try {
      const el = this.messageList.nativeElement;
      el.scrollTop = el.scrollHeight;
    } catch {}
  }
}
