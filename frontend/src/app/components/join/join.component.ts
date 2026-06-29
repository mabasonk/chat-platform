import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-join',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './join.component.html',
  styleUrls: ['./join.component.css']
})
export class JoinComponent {
  @Output() joined = new EventEmitter<string>();
  username = '';

  join(): void {
    const name = this.username.trim();
    if (name) this.joined.emit(name);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.join();
  }
}
