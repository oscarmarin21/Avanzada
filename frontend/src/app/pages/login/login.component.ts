import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  private auth = inject(AuthService);
  private router = inject(Router);

  identifier = '';
  password = '';
  error = '';
  loading = false;

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
      this.error = 'Identifier and password are required.';
      return;
    }
    this.loading = true;
    this.auth.login(id, pwd).subscribe({
      next: res => {
        this.loading = false;
        if (res) {
          this.router.navigate(['/']);
        } else {
          this.error = 'Invalid identifier or password.';
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Login failed. Please try again.';
      }
    });
  }
}
