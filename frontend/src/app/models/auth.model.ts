export interface AuthUser {
  id: number;
  identifier: string;
  name: string;
  role: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

/** JWT payload (decode only; verification is server-side). */
export interface JwtPayload {
  sub: string;
  userId: number;
  role: string;
  exp: number;
  iat: number;
}
