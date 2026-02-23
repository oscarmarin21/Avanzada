import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RequestApiService } from '../../services/request-api.service';
import { AuthService } from '../../services/auth.service';
import { RequestTypeDto, ChannelDto, UserDto } from '../../models/request.model';

@Component({
  selector: 'app-request-create',
  standalone: true,
  imports: [RouterLink, FormsModule, TranslateModule],
  templateUrl: './request-create.component.html'
})
export class RequestCreateComponent implements OnInit {
  private router = inject(Router);
  private api = inject(RequestApiService);
  private translate = inject(TranslateService);
  readonly auth = inject(AuthService);

  requestTypes = signal<RequestTypeDto[]>([]);
  channels = signal<ChannelDto[]>([]);
  users = signal<UserDto[]>([]);
  loading = signal(true);
  submitting = signal(false);
  error = signal<string | null>(null);
  suggestLoading = signal(false);
  suggestMessage = signal<string | null>(null);
  aiAvailable = signal(false);

  form = {
    description: '',
    requestTypeId: 0,
    channelId: 0,
    requestedById: 0
  };

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (user) {
      this.form.requestedById = user.id;
    }
    this.api.getRequestTypes().subscribe(rt => this.requestTypes.set(rt));
    this.api.getChannels().subscribe(c => this.channels.set(c));
    this.api.getUsers().subscribe(u => this.users.set(u));
    this.api.getAiStatus().subscribe(s => this.aiAvailable.set(s.available));
    this.loading.set(false);
  }

  suggestType(): void {
    const d = this.form.description?.trim();
    if (!d) {
      this.suggestMessage.set(this.translate.instant('requestCreate.suggest.enterDescriptionFirst'));
      return;
    }
    this.suggestMessage.set(null);
    this.suggestLoading.set(true);
    this.api.suggestTypeAndPriority(d).subscribe(res => {
      this.suggestLoading.set(false);
      if (res.available && res.suggestedRequestTypeCode) {
        const rt = this.requestTypes().find(t => t.code === res.suggestedRequestTypeCode);
        if (rt) {
          this.form.requestTypeId = rt.id;
          this.suggestMessage.set(this.translate.instant('requestCreate.suggest.suggestedType', { name: rt.name }));
        } else {
          this.suggestMessage.set(this.translate.instant('requestCreate.suggest.typeNotFound'));
        }
      } else {
        this.suggestMessage.set(res.message ?? this.translate.instant('requestCreate.suggest.unavailable'));
      }
    });
  }

  submit(): void {
    const d = this.form.description?.trim();
    if (!d || !this.form.requestTypeId || !this.form.channelId || !this.form.requestedById) {
      this.error.set(this.translate.instant('requestCreate.errors.requiredFields'));
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
          this.error.set(this.translate.instant('requestCreate.errors.createFailed'));
        }
      },
      error: () => {
        this.submitting.set(false);
        this.error.set(this.translate.instant('requestCreate.errors.createFailed'));
      }
    });
  }
}
