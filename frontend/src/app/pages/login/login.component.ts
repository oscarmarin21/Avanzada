import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  private auth = inject(AuthService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  identifier = '';
  password = '';
  error = '';
  loading = false;

  get currentLang(): string {
    return this.translate.currentLang ?? 'es';
  }

  private static readonly LANG_STORAGE_KEY = 'avanzada_lang';
  private static readonly VALID_LANGS = ['es', 'en'] as const;

  setLang(lang: string): void {
    if (LoginComponent.VALID_LANGS.includes(lang as 'es' | 'en')) {
      this.translate.use(lang);
      localStorage.setItem(LoginComponent.LANG_STORAGE_KEY, lang);
    }
  }

  constructor() {
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/']);
    }
  }

  submit(): void {
    this.error = '';
    const id = this.identifier?.trim();
    const pwd = this.password;
    if (!id || !pwd) {
      this.error = this.translate.instant('login.errors.required');
      return;
    }
    this.loading = true;
    this.auth.login(id, pwd).subscribe({
      next: res => {
        this.loading = false;
        if (res) {
          this.router.navigate(['/']);
        } else {
          this.error = this.translate.instant('login.errors.invalid');
        }
      },
      error: () => {
        this.loading = false;
        this.error = this.translate.instant('login.errors.failed');
      }
    });
  }
}
