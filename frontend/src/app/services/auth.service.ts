import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { AuthUser, LoginRequest, LoginResponse, JwtPayload } from '../models/auth.model';
import { environment } from '../../environments/environment';

const API = environment.apiUrl;
const TOKEN_KEY = 'avanzada_token';
const USER_KEY = 'avanzada_user';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly currentUser = signal<AuthUser | null>(this.loadStoredUser());
  private readonly token = signal<string | null>(this.loadStoredToken());

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => !!this.token());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  getToken(): string | null {
    return this.token();
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUser();
  }

  hasRole(role: string): boolean {
    const u = this.currentUser();
    return u?.role === role || false;
  }

  hasAnyRole(...roles: string[]): boolean {
    const u = this.currentUser();
    if (!u?.role) return false;
    return roles.includes(u.role);
  }

  /** Can register new requests (STUDENT, STAFF, ADMIN). */
  canRegister(): boolean {
    return this.hasAnyRole('STUDENT', 'STAFF', 'ADMIN');
  }

  /** Can classify, assign, attend (STAFF, ADMIN). */
  canClassifyOrAssign(): boolean {
    return this.hasAnyRole('STAFF', 'ADMIN');
  }

  /** Can close requests (ADMIN only). */
  canClose(): boolean {
    return this.hasRole('ADMIN');
  }

  login(identifier: string, password: string): Observable<LoginResponse | null> {
    const body: LoginRequest = { identifier, password };
    return this.http.post<LoginResponse>(`${API}/auth/login`, body).pipe(
      tap(res => {
        if (res?.token && res?.user) {
          sessionStorage.setItem(TOKEN_KEY, res.token);
          sessionStorage.setItem(USER_KEY, JSON.stringify(res.user));
          this.token.set(res.token);
          this.currentUser.set(res.user);
        }
      }),
      catchError(() => of(null))
    );
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  private loadStoredToken(): string | null {
    if (typeof sessionStorage === 'undefined') return null;
    return sessionStorage.getItem(TOKEN_KEY);
  }

  private loadStoredUser(): AuthUser | null {
    if (typeof sessionStorage === 'undefined') return null;
    const raw = sessionStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}
