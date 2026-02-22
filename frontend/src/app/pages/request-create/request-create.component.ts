import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RequestApiService } from '../../services/request-api.service';
import { RequestTypeDto, ChannelDto, UserDto } from '../../models/request.model';

@Component({
  selector: 'app-request-create',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './request-create.component.html'
})
export class RequestCreateComponent implements OnInit {
  private router = inject(Router);
  private api = inject(RequestApiService);

  requestTypes = signal<RequestTypeDto[]>([]);
  channels = signal<ChannelDto[]>([]);
  users = signal<UserDto[]>([]);
  loading = signal(true);
  submitting = signal(false);
  error = signal<string | null>(null);

  form = {
    description: '',
    requestTypeId: 0,
    channelId: 0,
    requestedById: 0
  };

  ngOnInit(): void {
    this.api.getRequestTypes().subscribe(rt => this.requestTypes.set(rt));
    this.api.getChannels().subscribe(c => this.channels.set(c));
    this.api.getUsers().subscribe(u => this.users.set(u));
    this.loading.set(false);
  }

  submit(): void {
    const d = this.form.description?.trim();
    if (!d || !this.form.requestTypeId || !this.form.channelId || !this.form.requestedById) {
      this.error.set('Please fill in all required fields.');
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    this.api.createRequest({
      description: d,
      requestTypeId: this.form.requestTypeId,
      channelId: this.form.channelId,
      requestedById: this.form.requestedById
    }).subscribe({
      next: created => {
        this.submitting.set(false);
        if (created) {
          this.router.navigate(['/requests', created.id]);
        } else {
          this.error.set('Failed to create request.');
        }
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Failed to create request.');
      }
    });
  }
}
