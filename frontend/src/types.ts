export type UserRole = "CUSTOMER" | "ANALYST" | "ADMIN";

export type UserStatus = "ACTIVE" | "LOCKED" | "DISABLED";

export type AccountStatus = "ACTIVE" | "BLOCKED" | "CLOSED";

export type PaymentDecision =
    | "PENDING"
    | "APPROVED"
    | "DECLINED"
    | "MANUAL_REVIEW"
    | "CANCELLED";

export type RiskAlertStatus =
    | "OPEN"
    | "IN_REVIEW"
    | "FALSE_POSITIVE"
    | "CONFIRMED_FRAUD"
    | "CLOSED";

export type RiskAlertSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
    validationErrors: Record<string, string> | null;
}

export interface LoginResponse {
    userId: number;
    fullName: string;
    email: string;
    role: UserRole;
    status: UserStatus;
    accessToken: string;
    tokenType: string;
    expiresInMinutes: number;
}

export interface UserResponse {
    id: number;
    fullName: string;
    email: string;
    role: UserRole;
    status: UserStatus;
    createdAt: string;
}

export interface AdminUserResponse {
    id: number;
    fullName: string;
    email: string;
    role: UserRole;
    status: UserStatus;
    createdAt: string;
    updatedAt: string;
}

export interface AccountResponse {
    id: number;
    ownerName: string;
    iban: string;
    currency: string;
    balance: number;
    dailyLimit: number;
    monthlyLimit: number;
    status: AccountStatus;
    createdAt: string;
    updatedAt: string;
}

export interface PaymentAuthorizationResponse {
    paymentId: number;
    decision: PaymentDecision;
    riskScore: number;
    reasons: string[];
    createdAt: string;
}

export interface RiskAlertResponse {
    id: number;
    paymentId: number;
    accountId: number;
    riskScore: number;
    severity: RiskAlertSeverity;
    status: RiskAlertStatus;
    reasonSummary: string;
    createdAt: string;
    updatedAt: string;
}

export interface FinancialEventResponse {
    id: number;
    eventType: string;
    entityType: string;
    entityId: number;
    payloadJson: string;
    createdBy: string;
    createdAt: string;
}