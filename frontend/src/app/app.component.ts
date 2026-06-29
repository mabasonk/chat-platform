import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JoinComponent } from './components/join/join.component';
import { ChatComponent } from './components/chat/chat.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, JoinComponent, ChatComponent],
  template: `
    <app-join *ngIf="!username" (joined)="onJoined($event)" />
    <app-chat *ngIf="username" [username]="username" />
  `
})
export class AppComponent {
  username = '';

  onJoined(name: string): void {
    this.username = name;
  }
}
