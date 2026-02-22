import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RequestApiService } from '../../services/request-api.service';
import {
  RequestResponse,
  HistoryEntryDto,
  ClassifyRequestDto,
  RequestTypeDto,
  UserDto
} from '../../models/request.model';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './request-detail.component.html',
  styleUrl: './request-detail.component.css'
})
export class RequestDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(RequestApiService);

  request = signal<RequestResponse | null>(null);
  history = signal<HistoryEntryDto[]>([]);
  requestTypes = signal<RequestTypeDto[]>([]);
  users = signal<UserDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  actionError = signal<string | null>(null);

  showClassify = signal(false);
  showAssign = signal(false);
  showAttend = signal(false);
  showClose = signal(false);

  classifyForm = { requestTypeId: 0, priority: 'MEDIUM', priorityJustification: '' };
  assignForm = { assignedToId: 0 };
  attendForm = { observations: '' };
  closeForm = { closureObservation: '' };

  canClassify = computed(() => this.request()?.stateCode === 'REGISTRADA');
  canAssign = computed(() => this.request()?.stateCode === 'CLASIFICADA');
  canAttend = computed(() => this.request()?.stateCode === 'EN_ATENCION');
  canClose = computed(() => this.request()?.stateCode === 'ATENDIDA');
  isClosed = computed(() => this.request()?.stateCode === 'CERRADA');

  ngOnInit(): void {
    this.api.getRequestTypes().subscribe(rt => this.requestTypes.set(rt));
    this.api.getUsers().subscribe(u => this.users.set(u));
    const id = this.route.snapshot.paramMap.get('id');
    const numId = id ? +id : 0;
    if (!numId) {
      this.router.navigate(['/']);
      return;
    }
    this.load(numId);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getRequest(id).subscribe(r => {
      this.request.set(r);
      this.loading.set(false);
      if (!r) {
        this.error.set('Request not found');
        return;
      }
      this.api.getHistory(id).subscribe(h => this.history.set(h));
      this.classifyForm.requestTypeId = r.requestTypeId;
    });
  }

  submitClassify(): void {
    const req = this.request();
    if (!req || !this.canClassify()) return;
    this.actionError.set(null);
    const dto: ClassifyRequestDto = {
      requestTypeId: this.classifyForm.requestTypeId,
      priority: this.classifyForm.priority,
      priorityJustification: this.classifyForm.priorityJustification || undefined
    };
    this.api.classify(req.id, dto).subscribe(updated => {
      if (updated) {
        this.request.set(updated);
        this.showClassify.set(false);
        this.load(req.id);
      } else {
        this.actionError.set('Classification failed');
      }
    });
  }

  submitAssign(): void {
    const req = this.request();
    if (!req || !this.canAssign() || !this.assignForm.assignedToId) return;
    this.actionError.set(null);
    this.api.assign(req.id, { assignedToId: this.assignForm.assignedToId }).subscribe(updated => {
      if (updated) {
        this.request.set(updated);
        this.showAssign.set(false);
        this.load(req.id);
      } else {
        this.actionError.set('Assignment failed');
      }
    });
  }

  submitAttend(): void {
    const req = this.request();
    if (!req || !this.canAttend()) return;
    this.actionError.set(null);
    this.api.attend(req.id, { observations: this.attendForm.observations || undefined }).subscribe(updated => {
      if (updated) {
        this.request.set(updated);
        this.showAttend.set(false);
        this.attendForm.observations = '';
        this.load(req.id);
      } else {
        this.actionError.set('Attend action failed');
      }
    });
  }

  submitClose(): void {
    const req = this.request();
    const obs = this.closeForm.closureObservation?.trim();
    if (!req || !this.canClose() || !obs) {
      this.actionError.set('Closure observation is required');
      return;
    }
    this.actionError.set(null);
    this.api.close(req.id, { closureObservation: obs }).subscribe(updated => {
      if (updated) {
        this.request.set(updated);
        this.showClose.set(false);
        this.closeForm.closureObservation = '';
        this.load(req.id);
      } else {
        this.actionError.set('Close failed');
      }
    });
  }
}
