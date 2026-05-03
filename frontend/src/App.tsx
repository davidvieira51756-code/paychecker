import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import {
    AlertTriangle,
    BadgeCheck,
    BarChart3,
    CheckCircle2,
    CreditCard,
    FileText,
    LayoutDashboard,
    Lock,
    LogOut,
    RefreshCw,
    ShieldCheck,
    Users,
    XCircle,
} from "lucide-react";

import "./App.css";
import { apiRequest } from "./api";
import type {
    AccountResponse,
    AdminUserResponse,
    FinancialEventResponse,
    LoginResponse,
    PageResponse,
    PaymentAuthorizationResponse,
    RiskAlertResponse,
    RiskAlertStatus,
    UserRole,
    UserStatus,
} from "./types";

type Page = "dashboard" | "accounts" | "payments" | "alerts" | "eventlog" | "users";

type LoginForm = {
    email: string;
    password: string;
};

type RegisterForm = {
    fullName: string;
    email: string;
    password: string;
};

type AccountForm = {
    ownerName: string;
    iban: string;
    currency: string;
    initialBalance: string;
    dailyLimit: string;
    monthlyLimit: string;
};

type PaymentForm = {
    accountId: string;
    amount: string;
    currency: string;
    beneficiaryIban: string;
    beneficiaryName: string;
    beneficiaryCountry: string;
};

const initialLoginForm: LoginForm = {
    email: "david@example.com",
    password: "Password123",
};

const initialRegisterForm: RegisterForm = {
    fullName: "",
    email: "",
    password: "Password123",
};

const initialAccountForm: AccountForm = {
    ownerName: "Demo Customer",
    iban: "",
    currency: "EUR",
    initialBalance: "10000.00",
    dailyLimit: "10000.00",
    monthlyLimit: "50000.00",
};

const initialPaymentForm: PaymentForm = {
    accountId: "",
    amount: "6000.00",
    currency: "EUR",
    beneficiaryIban: "PT50008800000000000000088",
    beneficiaryName: "New Large Beneficiary",
    beneficiaryCountry: "PT",
};

function App() {
    const [auth, setAuth] = useState<LoginResponse | null>(() => {
        const stored = localStorage.getItem("paychecker.auth");
        return stored ? (JSON.parse(stored) as LoginResponse) : null;
    });

    const [activePage, setActivePage] = useState<Page>("dashboard");
    const [authMode, setAuthMode] = useState<"login" | "register">("login");

    const [loginForm, setLoginForm] = useState<LoginForm>(initialLoginForm);
    const [registerForm, setRegisterForm] = useState<RegisterForm>(initialRegisterForm);

    const [accountForm, setAccountForm] = useState<AccountForm>({
        ...initialAccountForm,
        iban: generateIban(),
    });

    const [paymentForm, setPaymentForm] = useState<PaymentForm>(initialPaymentForm);

    const [accounts, setAccounts] = useState<AccountResponse[]>([]);
    const [alerts, setAlerts] = useState<RiskAlertResponse[]>([]);
    const [events, setEvents] = useState<FinancialEventResponse[]>([]);
    const [users, setUsers] = useState<AdminUserResponse[]>([]);

    const [paymentResult, setPaymentResult] = useState<PaymentAuthorizationResponse | null>(null);

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const token = auth?.accessToken;

    const canViewAlerts = auth?.role === "ANALYST" || auth?.role === "ADMIN";
    const canViewEventLog = auth?.role === "ADMIN";
    const canManageUsers = auth?.role === "ADMIN";

    const totalBalance = useMemo(
        () => accounts.reduce((sum, account) => sum + Number(account.balance), 0),
        [accounts]
    );

    useEffect(() => {
        if (token) {
            void loadData();
        }
    }, [token, activePage]);

    async function loadData() {
        if (!token) return;

        setLoading(true);
        setError("");

        try {
            if (activePage === "dashboard" || activePage === "accounts" || activePage === "payments") {
                const accountsPage = await apiRequest<PageResponse<AccountResponse>>(
                    "/api/accounts?page=0&size=20&sort=createdAt,desc",
                    {},
                    token
                );

                setAccounts(accountsPage.content);

                if (!paymentForm.accountId && accountsPage.content.length > 0) {
                    setPaymentForm((current) => ({
                        ...current,
                        accountId: String(accountsPage.content[0].id),
                    }));
                }
            }

            if ((activePage === "dashboard" || activePage === "alerts") && canViewAlerts) {
                const alertsPage = await apiRequest<PageResponse<RiskAlertResponse>>(
                    "/api/alerts?page=0&size=20&sort=createdAt,desc",
                    {},
                    token
                );

                setAlerts(alertsPage.content);
            }

            if ((activePage === "dashboard" || activePage === "eventlog") && canViewEventLog) {
                const eventsPage = await apiRequest<PageResponse<FinancialEventResponse>>(
                    "/api/event-log?page=0&size=30&sort=createdAt,desc",
                    {},
                    token
                );

                setEvents(eventsPage.content);
            }

            if (activePage === "users" && canManageUsers) {
                const usersPage = await apiRequest<PageResponse<AdminUserResponse>>(
                    "/api/admin/users?page=0&size=30&sort=createdAt,desc",
                    {},
                    token
                );

                setUsers(usersPage.content);
            }
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Failed to load data");
        } finally {
            setLoading(false);
        }
    }

    async function handleLogin(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        setError("");
        setMessage("");

        try {
            const response = await apiRequest<LoginResponse>("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(loginForm),
            });

            localStorage.setItem("paychecker.auth", JSON.stringify(response));
            setAuth(response);
            setActivePage("dashboard");
            setMessage(`Welcome back, ${response.fullName}`);
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Login failed");
        }
    }

    async function handleRegister(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        setError("");
        setMessage("");

        try {
            await apiRequest("/api/auth/register", {
                method: "POST",
                body: JSON.stringify(registerForm),
            });

            setMessage("User registered successfully. You can now sign in.");
            setAuthMode("login");
            setLoginForm({
                email: registerForm.email,
                password: registerForm.password,
            });
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Registration failed");
        }
    }

    function handleLogout() {
        localStorage.removeItem("paychecker.auth");
        setAuth(null);
        setAccounts([]);
        setAlerts([]);
        setEvents([]);
        setUsers([]);
        setPaymentResult(null);
        setMessage("");
        setError("");
    }

    async function createAccount(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!token) return;

        setError("");
        setMessage("");

        try {
            await apiRequest<AccountResponse>(
                "/api/accounts",
                {
                    method: "POST",
                    body: JSON.stringify({
                        ownerName: accountForm.ownerName,
                        iban: accountForm.iban,
                        currency: accountForm.currency,
                        initialBalance: Number(accountForm.initialBalance),
                        dailyLimit: Number(accountForm.dailyLimit),
                        monthlyLimit: Number(accountForm.monthlyLimit),
                    }),
                },
                token
            );

            setMessage("Account created successfully.");
            setAccountForm({
                ...initialAccountForm,
                iban: generateIban(),
            });

            await loadData();
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Failed to create account");
        }
    }

    async function authorizePayment(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!token) return;

        setError("");
        setMessage("");
        setPaymentResult(null);

        try {
            const response = await apiRequest<PaymentAuthorizationResponse>(
                "/api/payments/authorize",
                {
                    method: "POST",
                    body: JSON.stringify({
                        accountId: Number(paymentForm.accountId),
                        amount: Number(paymentForm.amount),
                        currency: paymentForm.currency,
                        beneficiaryIban: paymentForm.beneficiaryIban,
                        beneficiaryName: paymentForm.beneficiaryName,
                        beneficiaryCountry: paymentForm.beneficiaryCountry,
                    }),
                },
                token
            );

            setPaymentResult(response);
            setMessage(`Payment decision: ${response.decision}`);

            await loadData();
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Payment authorization failed");
        }
    }

    async function updateAlertStatus(alertId: number, status: RiskAlertStatus) {
        if (!token) return;

        setError("");
        setMessage("");

        try {
            await apiRequest<RiskAlertResponse>(
                `/api/alerts/${alertId}/status`,
                {
                    method: "PATCH",
                    body: JSON.stringify({ status }),
                },
                token
            );

            setMessage(`Alert #${alertId} updated to ${status}.`);
            await loadData();
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Failed to update alert");
        }
    }

    async function updateUserRole(userId: number, role: UserRole) {
        if (!token) return;

        setError("");
        setMessage("");

        try {
            await apiRequest<AdminUserResponse>(
                `/api/admin/users/${userId}/role`,
                {
                    method: "PATCH",
                    body: JSON.stringify({ role }),
                },
                token
            );

            setMessage(`User #${userId} role updated to ${role}.`);
            await loadData();
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Failed to update user role");
        }
    }

    async function updateUserStatus(userId: number, status: UserStatus) {
        if (!token) return;

        setError("");
        setMessage("");

        try {
            await apiRequest<AdminUserResponse>(
                `/api/admin/users/${userId}/status`,
                {
                    method: "PATCH",
                    body: JSON.stringify({ status }),
                },
                token
            );

            setMessage(`User #${userId} status updated to ${status}.`);
            await loadData();
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "Failed to update user status");
        }
    }

    if (!auth) {
        return (
            <main className="auth-page">
                <section className="auth-card">
                    <div className="brand-row">
                        <div className="brand-icon">
                            <ShieldCheck size={34} />
                        </div>

                        <div>
                            <h1>PayChecker</h1>
                            <p>Secure payment authorization and fraud risk operations.</p>
                        </div>
                    </div>

                    <div className="auth-tabs">
                        <button
                            className={authMode === "login" ? "active" : ""}
                            onClick={() => setAuthMode("login")}
                        >
                            Login
                        </button>
                        <button
                            className={authMode === "register" ? "active" : ""}
                            onClick={() => setAuthMode("register")}
                        >
                            Register
                        </button>
                    </div>

                    {authMode === "login" ? (
                        <form className="form-stack" onSubmit={handleLogin}>
                            <label>
                                Email
                                <input
                                    value={loginForm.email}
                                    onChange={(event) =>
                                        setLoginForm({ ...loginForm, email: event.target.value })
                                    }
                                    placeholder="david@example.com"
                                />
                            </label>

                            <label>
                                Password
                                <input
                                    type="password"
                                    value={loginForm.password}
                                    onChange={(event) =>
                                        setLoginForm({ ...loginForm, password: event.target.value })
                                    }
                                    placeholder="Password123"
                                />
                            </label>

                            <button className="primary-button" type="submit">
                                Sign in
                            </button>
                        </form>
                    ) : (
                        <form className="form-stack" onSubmit={handleRegister}>
                            <label>
                                Full name
                                <input
                                    value={registerForm.fullName}
                                    onChange={(event) =>
                                        setRegisterForm({ ...registerForm, fullName: event.target.value })
                                    }
                                    placeholder="Jane Doe"
                                />
                            </label>

                            <label>
                                Email
                                <input
                                    value={registerForm.email}
                                    onChange={(event) =>
                                        setRegisterForm({ ...registerForm, email: event.target.value })
                                    }
                                    placeholder="jane@example.com"
                                />
                            </label>

                            <label>
                                Password
                                <input
                                    type="password"
                                    value={registerForm.password}
                                    onChange={(event) =>
                                        setRegisterForm({ ...registerForm, password: event.target.value })
                                    }
                                    placeholder="Password123"
                                />
                            </label>

                            <button className="primary-button" type="submit">
                                Create account
                            </button>
                        </form>
                    )}

                    {message && <div className="success-box">{message}</div>}
                    {error && <div className="error-box">{error}</div>}
                </section>
            </main>
        );
    }

    return (
        <main className="app-shell">
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <ShieldCheck size={28} />
                    <span>PayChecker</span>
                </div>

                <nav className="sidebar-nav">
                    <NavButton
                        page="dashboard"
                        activePage={activePage}
                        setActivePage={setActivePage}
                        icon={<LayoutDashboard size={18} />}
                        label="Dashboard"
                    />
                    <NavButton
                        page="accounts"
                        activePage={activePage}
                        setActivePage={setActivePage}
                        icon={<CreditCard size={18} />}
                        label="Accounts"
                    />
                    <NavButton
                        page="payments"
                        activePage={activePage}
                        setActivePage={setActivePage}
                        icon={<BarChart3 size={18} />}
                        label="Payments"
                    />
                    <NavButton
                        page="alerts"
                        activePage={activePage}
                        setActivePage={setActivePage}
                        icon={<AlertTriangle size={18} />}
                        label="Risk Alerts"
                    />
                    <NavButton
                        page="eventlog"
                        activePage={activePage}
                        setActivePage={setActivePage}
                        icon={<FileText size={18} />}
                        label="Event Log"
                    />
                    <NavButton
                        page="users"
                        activePage={activePage}
                        setActivePage={setActivePage}
                        icon={<Users size={18} />}
                        label="Users"
                    />
                </nav>

                <div className="sidebar-profile">
                    <div className="avatar">{auth.fullName.charAt(0).toUpperCase()}</div>
                    <div>
                        <strong>{auth.fullName}</strong>
                        <span>{auth.role}</span>
                    </div>
                </div>
            </aside>

            <section className="main-content">
                <header className="topbar">
                    <div>
                        <p className="eyebrow">Fintech Risk Operations</p>
                        <h1>{pageTitle(activePage)}</h1>
                    </div>

                    <div className="topbar-actions">
                        <button className="ghost-button" onClick={loadData}>
                            <RefreshCw size={16} />
                            Refresh
                        </button>

                        <button className="ghost-button danger" onClick={handleLogout}>
                            <LogOut size={16} />
                            Logout
                        </button>
                    </div>
                </header>

                {message && <div className="success-box">{message}</div>}
                {error && <div className="error-box">{error}</div>}
                {loading && <div className="loading-box">Loading data...</div>}

                {activePage === "dashboard" && (
                    <DashboardPage
                        accounts={accounts}
                        alerts={alerts}
                        events={events}
                        role={auth.role}
                        totalBalance={totalBalance}
                        canViewAlerts={canViewAlerts}
                        canViewEventLog={canViewEventLog}
                        setActivePage={setActivePage}
                    />
                )}

                {activePage === "accounts" && (
                    <AccountsPage
                        accounts={accounts}
                        accountForm={accountForm}
                        setAccountForm={setAccountForm}
                        createAccount={createAccount}
                    />
                )}

                {activePage === "payments" && (
                    <PaymentsPage
                        accounts={accounts}
                        paymentForm={paymentForm}
                        setPaymentForm={setPaymentForm}
                        paymentResult={paymentResult}
                        authorizePayment={authorizePayment}
                    />
                )}

                {activePage === "alerts" && (
                    <AlertsPage
                        canViewAlerts={canViewAlerts}
                        alerts={alerts}
                        updateAlertStatus={updateAlertStatus}
                    />
                )}

                {activePage === "eventlog" && (
                    <EventLogPage canViewEventLog={canViewEventLog} events={events} />
                )}

                {activePage === "users" && (
                    <UsersPage
                        canManageUsers={canManageUsers}
                        users={users}
                        updateUserRole={updateUserRole}
                        updateUserStatus={updateUserStatus}
                    />
                )}
            </section>
        </main>
    );
}

function DashboardPage({
                           accounts,
                           alerts,
                           events,
                           role,
                           totalBalance,
                           canViewAlerts,
                           canViewEventLog,
                           setActivePage,
                       }: {
    accounts: AccountResponse[];
    alerts: RiskAlertResponse[];
    events: FinancialEventResponse[];
    role: UserRole;
    totalBalance: number;
    canViewAlerts: boolean;
    canViewEventLog: boolean;
    setActivePage: (page: Page) => void;
}) {
    return (
        <>
            <section className="kpi-grid">
                <KpiCard
                    title="Accounts"
                    value={String(accounts.length)}
                    label="Loaded accounts"
                    icon={<CreditCard />}
                    tone="blue"
                />

                <KpiCard
                    title="Total Balance"
                    value={`€${formatMoney(totalBalance)}`}
                    label="Across loaded accounts"
                    icon={<BadgeCheck />}
                    tone="green"
                />

                <KpiCard
                    title="Risk Alerts"
                    value={canViewAlerts ? String(alerts.length) : "Restricted"}
                    label={canViewAlerts ? "Analyst/Admin access" : "Requires analyst role"}
                    icon={<AlertTriangle />}
                    tone="amber"
                />

                <KpiCard
                    title="Event Log"
                    value={canViewEventLog ? String(events.length) : "Restricted"}
                    label={canViewEventLog ? "Admin audit access" : "Requires admin role"}
                    icon={<FileText />}
                    tone="purple"
                />
            </section>

            <section className="content-grid">
                <article className="panel hero-panel">
                    <div>
                        <p className="eyebrow">Current Role</p>
                        <h2>{role}</h2>
                        <p>
                            PayChecker combines payment authorization, risk scoring, security logging and
                            operational review flows in a single fintech backend.
                        </p>
                    </div>

                    <div className="hero-actions">
                        <button className="primary-button" onClick={() => setActivePage("payments")}>
                            Authorize Payment
                        </button>
                        <button className="ghost-button" onClick={() => setActivePage("accounts")}>
                            Manage Accounts
                        </button>
                    </div>
                </article>

                <article className="panel">
                    <PanelTitle title="Recent Events" subtitle="Admin-only audit feed" />
                    {!canViewEventLog && <Restricted message="Event log requires ADMIN role." />}

                    {canViewEventLog && (
                        <div className="event-feed">
                            {events.slice(0, 6).map((event) => (
                                <EventItem event={event} key={event.id} />
                            ))}
                        </div>
                    )}
                </article>
            </section>
        </>
    );
}

function AccountsPage({
                          accounts,
                          accountForm,
                          setAccountForm,
                          createAccount,
                      }: {
    accounts: AccountResponse[];
    accountForm: AccountForm;
    setAccountForm: (form: AccountForm) => void;
    createAccount: (event: FormEvent<HTMLFormElement>) => void;
}) {
    return (
        <section className="content-grid">
            <article className="panel">
                <PanelTitle title="Create Account" subtitle="Create a demo account for payment testing." />

                <form className="form-grid" onSubmit={createAccount}>
                    <TextInput
                        label="Owner name"
                        value={accountForm.ownerName}
                        onChange={(value) => setAccountForm({ ...accountForm, ownerName: value })}
                    />
                    <TextInput
                        label="IBAN"
                        value={accountForm.iban}
                        onChange={(value) => setAccountForm({ ...accountForm, iban: value })}
                    />
                    <TextInput
                        label="Currency"
                        value={accountForm.currency}
                        onChange={(value) => setAccountForm({ ...accountForm, currency: value })}
                    />
                    <TextInput
                        label="Initial balance"
                        value={accountForm.initialBalance}
                        onChange={(value) => setAccountForm({ ...accountForm, initialBalance: value })}
                    />
                    <TextInput
                        label="Daily limit"
                        value={accountForm.dailyLimit}
                        onChange={(value) => setAccountForm({ ...accountForm, dailyLimit: value })}
                    />
                    <TextInput
                        label="Monthly limit"
                        value={accountForm.monthlyLimit}
                        onChange={(value) => setAccountForm({ ...accountForm, monthlyLimit: value })}
                    />

                    <button className="primary-button full-span" type="submit">
                        Create Account
                    </button>
                </form>
            </article>

            <article className="panel">
                <PanelTitle title="Accounts" subtitle="Accounts available to the authenticated user." />

                <div className="table-list">
                    {accounts.length === 0 && <Empty message="No accounts found." />}

                    {accounts.map((account) => (
                        <div className="table-row" key={account.id}>
                            <div>
                                <strong>{account.ownerName}</strong>
                                <span>{account.iban}</span>
                            </div>

                            <div className="right">
                                <strong>
                                    {account.currency} {formatMoney(account.balance)}
                                </strong>
                                <span className="pill pill-green">{account.status}</span>
                            </div>
                        </div>
                    ))}
                </div>
            </article>
        </section>
    );
}

function PaymentsPage({
                          accounts,
                          paymentForm,
                          setPaymentForm,
                          paymentResult,
                          authorizePayment,
                      }: {
    accounts: AccountResponse[];
    paymentForm: PaymentForm;
    setPaymentForm: (form: PaymentForm) => void;
    paymentResult: PaymentAuthorizationResponse | null;
    authorizePayment: (event: FormEvent<HTMLFormElement>) => void;
}) {
    return (
        <section className="content-grid">
            <article className="panel">
                <PanelTitle
                    title="Authorize Payment"
                    subtitle="Send a payment request through validation and risk scoring."
                />

                <form className="form-grid" onSubmit={authorizePayment}>
                    <label>
                        Account
                        <select
                            value={paymentForm.accountId}
                            onChange={(event) =>
                                setPaymentForm({ ...paymentForm, accountId: event.target.value })
                            }
                        >
                            <option value="">Select account</option>
                            {accounts.map((account) => (
                                <option key={account.id} value={account.id}>
                                    #{account.id} · {account.ownerName} · {account.currency}{" "}
                                    {formatMoney(account.balance)}
                                </option>
                            ))}
                        </select>
                    </label>

                    <TextInput
                        label="Amount"
                        value={paymentForm.amount}
                        onChange={(value) => setPaymentForm({ ...paymentForm, amount: value })}
                    />
                    <TextInput
                        label="Currency"
                        value={paymentForm.currency}
                        onChange={(value) => setPaymentForm({ ...paymentForm, currency: value })}
                    />
                    <TextInput
                        label="Beneficiary IBAN"
                        value={paymentForm.beneficiaryIban}
                        onChange={(value) => setPaymentForm({ ...paymentForm, beneficiaryIban: value })}
                    />
                    <TextInput
                        label="Beneficiary name"
                        value={paymentForm.beneficiaryName}
                        onChange={(value) => setPaymentForm({ ...paymentForm, beneficiaryName: value })}
                    />
                    <TextInput
                        label="Beneficiary country"
                        value={paymentForm.beneficiaryCountry}
                        onChange={(value) => setPaymentForm({ ...paymentForm, beneficiaryCountry: value })}
                    />

                    <button className="primary-button full-span" type="submit">
                        Run Authorization
                    </button>
                </form>
            </article>

            <article className="panel">
                <PanelTitle title="Decision Result" subtitle="Authorization result from the backend." />

                {!paymentResult && <Empty message="No payment decision yet." />}

                {paymentResult && (
                    <div className={`decision-card decision-${paymentResult.decision.toLowerCase()}`}>
                        <div className="decision-icon">{decisionIcon(paymentResult.decision)}</div>

                        <div>
                            <p className="muted">Decision</p>
                            <h2>{paymentResult.decision}</h2>
                        </div>

                        <div>
                            <p className="muted">Risk Score</p>
                            <h2>{paymentResult.riskScore}/100</h2>
                        </div>

                        <div className="reason-list">
                            {paymentResult.reasons.map((reason) => (
                                <span key={reason}>{reason}</span>
                            ))}
                        </div>
                    </div>
                )}
            </article>
        </section>
    );
}

function AlertsPage({
                        canViewAlerts,
                        alerts,
                        updateAlertStatus,
                    }: {
    canViewAlerts: boolean;
    alerts: RiskAlertResponse[];
    updateAlertStatus: (alertId: number, status: RiskAlertStatus) => Promise<void>;
}) {
    if (!canViewAlerts) {
        return <Restricted message="Risk alerts require ANALYST or ADMIN role." />;
    }

    return (
        <article className="panel">
            <PanelTitle title="Risk Alerts" subtitle="Review and update alerts generated by risk scoring." />

            <div className="table-list">
                {alerts.length === 0 && <Empty message="No risk alerts found." />}

                {alerts.map((alert) => (
                    <div className="table-row alert-row" key={alert.id}>
                        <div>
                            <strong>Alert #{alert.id}</strong>
                            <span>
                Payment #{alert.paymentId} · Account #{alert.accountId}
              </span>
                            <span>{alert.reasonSummary}</span>
                        </div>

                        <div className="right">
                            <strong>{alert.riskScore}/100</strong>
                            <span className={`pill ${severityClass(alert.severity)}`}>{alert.severity}</span>

                            <select
                                value={alert.status}
                                onChange={(event) =>
                                    updateAlertStatus(alert.id, event.target.value as RiskAlertStatus)
                                }
                            >
                                <option value="OPEN">OPEN</option>
                                <option value="IN_REVIEW">IN_REVIEW</option>
                                <option value="FALSE_POSITIVE">FALSE_POSITIVE</option>
                                <option value="CONFIRMED_FRAUD">CONFIRMED_FRAUD</option>
                                <option value="CLOSED">CLOSED</option>
                            </select>
                        </div>
                    </div>
                ))}
            </div>
        </article>
    );
}

function EventLogPage({
                          canViewEventLog,
                          events,
                      }: {
    canViewEventLog: boolean;
    events: FinancialEventResponse[];
}) {
    if (!canViewEventLog) {
        return <Restricted message="Event log requires ADMIN role." />;
    }

    return (
        <article className="panel">
            <PanelTitle title="Event Log" subtitle="Append-only financial and security audit trail." />

            <div className="event-feed">
                {events.length === 0 && <Empty message="No events found." />}

                {events.map((event) => (
                    <EventItem event={event} key={event.id} />
                ))}
            </div>
        </article>
    );
}

function UsersPage({
                       canManageUsers,
                       users,
                       updateUserRole,
                       updateUserStatus,
                   }: {
    canManageUsers: boolean;
    users: AdminUserResponse[];
    updateUserRole: (userId: number, role: UserRole) => Promise<void>;
    updateUserStatus: (userId: number, status: UserStatus) => Promise<void>;
}) {
    if (!canManageUsers) {
        return <Restricted message="User management requires ADMIN role." />;
    }

    return (
        <article className="panel">
            <PanelTitle
                title="Admin Users"
                subtitle="Manage user roles and account status for the PayChecker platform."
            />

            <div className="table-list">
                {users.length === 0 && <Empty message="No users found." />}

                {users.map((user) => (
                    <div className="table-row user-row" key={user.id}>
                        <div>
                            <strong>{user.fullName}</strong>
                            <span>{user.email}</span>
                            <span>
                Created {new Date(user.createdAt).toLocaleDateString()} · ID #{user.id}
              </span>
                        </div>

                        <div className="user-controls">
                            <select
                                value={user.role}
                                onChange={(event) => updateUserRole(user.id, event.target.value as UserRole)}
                            >
                                <option value="CUSTOMER">CUSTOMER</option>
                                <option value="ANALYST">ANALYST</option>
                                <option value="ADMIN">ADMIN</option>
                            </select>

                            <select
                                value={user.status}
                                onChange={(event) => updateUserStatus(user.id, event.target.value as UserStatus)}
                            >
                                <option value="ACTIVE">ACTIVE</option>
                                <option value="LOCKED">LOCKED</option>
                                <option value="DISABLED">DISABLED</option>
                            </select>
                        </div>
                    </div>
                ))}
            </div>
        </article>
    );
}

function NavButton({
                       page,
                       activePage,
                       setActivePage,
                       icon,
                       label,
                   }: {
    page: Page;
    activePage: Page;
    setActivePage: (page: Page) => void;
    icon: ReactNode;
    label: string;
}) {
    return (
        <button
            className={activePage === page ? "nav-button active" : "nav-button"}
            onClick={() => setActivePage(page)}
        >
            {icon}
            {label}
        </button>
    );
}

function KpiCard({
                     title,
                     value,
                     label,
                     icon,
                     tone,
                 }: {
    title: string;
    value: string;
    label: string;
    icon: ReactNode;
    tone: "blue" | "green" | "amber" | "purple";
}) {
    return (
        <article className={`kpi-card kpi-${tone}`}>
            <div className="kpi-icon">{icon}</div>
            <div>
                <p>{title}</p>
                <h2>{value}</h2>
                <span>{label}</span>
            </div>
        </article>
    );
}

function PanelTitle({ title, subtitle }: { title: string; subtitle: string }) {
    return (
        <div className="panel-title">
            <div>
                <h2>{title}</h2>
                <p>{subtitle}</p>
            </div>
        </div>
    );
}

function TextInput({
                       label,
                       value,
                       onChange,
                   }: {
    label: string;
    value: string;
    onChange: (value: string) => void;
}) {
    return (
        <label>
            {label}
            <input value={value} onChange={(event) => onChange(event.target.value)} />
        </label>
    );
}

function Empty({ message }: { message: string }) {
    return <div className="empty-box">{message}</div>;
}

function Restricted({ message }: { message: string }) {
    return (
        <div className="restricted-box">
            <Lock size={26} />
            <h2>Restricted Area</h2>
            <p>{message}</p>
        </div>
    );
}

function EventItem({ event }: { event: FinancialEventResponse }) {
    return (
        <div className="event-item">
            <span className="event-dot" />
            <div>
                <strong>{event.eventType}</strong>
                <p>
                    {event.entityType} #{event.entityId} · {new Date(event.createdAt).toLocaleString()}
                </p>
                <code>{event.payloadJson}</code>
            </div>
        </div>
    );
}

function decisionIcon(decision: string) {
    if (decision === "APPROVED") return <CheckCircle2 size={28} />;
    if (decision === "DECLINED") return <XCircle size={28} />;
    return <AlertTriangle size={28} />;
}

function severityClass(severity: string) {
    if (severity === "CRITICAL") return "pill-red";
    if (severity === "HIGH") return "pill-amber";
    if (severity === "MEDIUM") return "pill-blue";
    return "pill-green";
}

function pageTitle(page: Page) {
    const titles: Record<Page, string> = {
        dashboard: "Risk & Payments Dashboard",
        accounts: "Accounts",
        payments: "Payment Authorization",
        alerts: "Risk Alerts",
        eventlog: "Event Log",
        users: "User Administration",
    };

    return titles[page];
}

function formatMoney(value: number) {
    return Number(value).toLocaleString("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
}

function generateIban() {
    const random = Math.floor(Math.random() * 999999999999)
        .toString()
        .padStart(12, "0");

    return `PT50${random}0000000001`.slice(0, 25);
}

export default App;