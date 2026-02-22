export interface RequestResponse {
  id: number;
  description: string;
  registeredAt: string;
  requestTypeId: number;
  requestTypeCode?: string;
  requestTypeName?: string;
  channelId: number;
  channelCode?: string;
  channelName?: string;
  stateId: number;
  stateCode?: string;
  stateName?: string;
  priority?: string;
  priorityJustification?: string;
  requestedById: number;
  requestedByIdentifier?: string;
  requestedByName?: string;
  assignedToId?: number | null;
  assignedToIdentifier?: string;
  assignedToName?: string;
  closureObservation?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRequestDto {
  description: string;
  requestTypeId: number;
  channelId: number;
  requestedById: number;
  registeredAt?: string;
}

export interface ClassifyRequestDto {
  requestTypeId: number;
  priority: string;
  priorityJustification?: string;
}

export interface AssignRequestDto {
  assignedToId: number;
}

export interface AttendRequestDto {
  observations?: string;
}

export interface CloseRequestDto {
  closureObservation: string;
}

export interface HistoryEntryDto {
  id: number;
  requestId: number;
  occurredAt: string;
  action: string;
  userId: number;
  userIdentifier?: string;
  userName?: string;
  observations?: string | null;
}

export interface RequestTypeDto {
  id: number;
  code: string;
  name: string;
  description?: string | null;
}

export interface ChannelDto {
  id: number;
  code: string;
  name: string;
}

export interface StateDto {
  id: number;
  code: string;
  name: string;
  order: number;
}

export interface UserDto {
  id: number;
  identifier: string;
  name: string;
  active: boolean;
}

export interface RequestListFilters {
  state?: string;
  requestType?: number;
  priority?: string;
  assignedTo?: number;
}
