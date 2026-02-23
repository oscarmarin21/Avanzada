import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { initFlowbite } from 'flowbite';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslateModule],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {

  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly translate = inject(TranslateService);
  private navSubscription?: Subscription;

  private static readonly LANG_STORAGE_KEY = 'avanzada_lang';
  private static readonly VALID_LANGS = ['es', 'en'] as const;

  readonly currentLang = (): string => this.translate.currentLang ?? 'es';

  setLang(lang: string): void {
    if (AppComponent.VALID_LANGS.includes(lang as 'es' | 'en')) {
      this.translate.use(lang);
      localStorage.setItem(AppComponent.LANG_STORAGE_KEY, lang);
    }
  }

  ngOnInit(): void {
    const saved = localStorage.getItem(AppComponent.LANG_STORAGE_KEY);
    if (saved && AppComponent.VALID_LANGS.includes(saved as 'es' | 'en')) {
      this.translate.use(saved);
    }
    initFlowbite();
    this.navSubscription = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(() => {
      setTimeout(() => initFlowbite(), 0);
    });
  }

  ngOnDestroy(): void {
    this.navSubscription?.unsubscribe();
  }

  logout(): void {
    this.auth.logout();
  }
}
