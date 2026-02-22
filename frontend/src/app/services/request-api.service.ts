import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import {
  RequestResponse,
  CreateRequestDto,
  ClassifyRequestDto,
  AssignRequestDto,
  AttendRequestDto,
  CloseRequestDto,
  HistoryEntryDto,
  RequestTypeDto,
  ChannelDto,
  StateDto,
  UserDto,
  RequestListFilters
} from '../models/request.model';

const API = '/api';

@Injectable({ providedIn: 'root' })
export class RequestApiService {

  constructor(private http: HttpClient) {}

  listRequests(filters?: RequestListFilters): Observable<RequestResponse[]> {
    let params = new HttpParams();
    if (filters?.state) params = params.set('state', filters.state);
    if (filters?.requestType != null) params = params.set('requestType', filters.requestType);
    if (filters?.priority) params = params.set('priority', filters.priority);
    if (filters?.assignedTo != null) params = params.set('assignedTo', filters.assignedTo);
    return this.http.get<RequestResponse[]>(`${API}/requests`, { params });
  }

  getRequest(id: number): Observable<RequestResponse | null> {
    return this.http.get<RequestResponse>(`${API}/requests/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  createRequest(dto: CreateRequestDto): Observable<RequestResponse | null> {
    return this.http.post<RequestResponse>(`${API}/requests`, dto, { observe: 'body' }).pipe(
      catchError(() => of(null))
    );
  }

  classify(id: number, dto: ClassifyRequestDto): Observable<RequestResponse | null> {
    return this.http.post<RequestResponse>(`${API}/requests/${id}/classify`, dto).pipe(
      catchError(() => of(null))
    );
  }

  assign(id: number, dto: AssignRequestDto): Observable<RequestResponse | null> {
    return this.http.post<RequestResponse>(`${API}/requests/${id}/assign`, dto).pipe(
      catchError(() => of(null))
    );
  }

  attend(id: number, dto?: AttendRequestDto): Observable<RequestResponse | null> {
    return this.http.post<RequestResponse>(`${API}/requests/${id}/attend`, dto ?? {}).pipe(
      catchError(() => of(null))
    );
  }

  close(id: number, dto: CloseRequestDto): Observable<RequestResponse | null> {
    return this.http.post<RequestResponse>(`${API}/requests/${id}/close`, dto).pipe(
      catchError(() => of(null))
    );
  }

  getHistory(id: number): Observable<HistoryEntryDto[]> {
    return this.http.get<HistoryEntryDto[]>(`${API}/requests/${id}/history`).pipe(
      catchError(() => of([]))
    );
  }

  getRequestTypes(): Observable<RequestTypeDto[]> {
    return this.http.get<RequestTypeDto[]>(`${API}/request-types`).pipe(
      catchError(() => of([]))
    );
  }

  getChannels(): Observable<ChannelDto[]> {
    return this.http.get<ChannelDto[]>(`${API}/channels`).pipe(
      catchError(() => of([]))
    );
  }

  getStates(): Observable<StateDto[]> {
    return this.http.get<StateDto[]>(`${API}/states`).pipe(
      catchError(() => of([]))
    );
  }

  getUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${API}/users`).pipe(
      catchError(() => of([]))
    );
  }
}
