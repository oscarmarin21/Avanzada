import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RequestApiService } from '../../services/request-api.service';
import { RequestResponse, RequestListFilters, StateDto, RequestTypeDto, UserDto } from '../../models/request.model';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

@Component({
  selector: 'app-request-list',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './request-list.component.html',
  styleUrl: './request-list.component.css'
})
export class RequestListComponent implements OnInit {
  private api = inject(RequestApiService);

  requests = signal<RequestResponse[]>([]);
  states = signal<StateDto[]>([]);
  requestTypes = signal<RequestTypeDto[]>([]);
  users = signal<UserDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  filterState = signal<string>('');
  filterRequestType = signal<number | ''>('');
  filterPriority = signal<string>('');
  filterAssignedTo = signal<number | ''>('');

  priorities = PRIORITIES;

  ngOnInit(): void {
    this.api.getStates().subscribe(s => this.states.set(s));
    this.api.getRequestTypes().subscribe(rt => this.requestTypes.set(rt));
    this.api.getUsers().subscribe(u => this.users.set(u));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const f: RequestListFilters = {};
    const state = this.filterState();
    if (state) f.state = state;
    const rt = this.filterRequestType();
    if (rt !== '') f.requestType = rt as number;
    const prio = this.filterPriority();
    if (prio) f.priority = prio;
    const assigned = this.filterAssignedTo();
    if (assigned !== '') f.assignedTo = assigned as number;
    this.api.listRequests(f).subscribe({
      next: list => {
        this.requests.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load requests');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.load();
  }

  clearFilters(): void {
    this.filterState.set('');
    this.filterRequestType.set('');
    this.filterPriority.set('');
    this.filterAssignedTo.set('');
    this.load();
  }
}
