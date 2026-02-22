import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { initFlowbite } from 'flowbite';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {

  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  private navSubscription?: Subscription;

  ngOnInit(): void {
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
