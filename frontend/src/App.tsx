import { useEffect, useMemo, useState } from "react";
import {
    AlertTriangle,
    CheckCircle2,
    Clock3,
    CreditCard,
    FileText,
    LogOut,
    RefreshCw,
    ShieldCheck,
    UserRound,
} from "lucide-react";

import "./App.css";
import { apiRequest } from "./api";
import type {
    AccountResponse,
    FinancialEventResponse,
    LoginResponse,
    PageResponse,
    PaymentAuthorizationResponse,
    RiskAlertResponse,
} from "./types";

type PaymentForm = {
    accountId: string;
    amount: string;
    currency: string;
    beneficiaryIban: string;
    beneficiaryName: string;
    beneficiaryCountry: string;
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

    const [loginEmail, setLoginEmail] = useState("david@example.com");
    const [loginPassword, setLoginPassword] = useState("Password123");
    const [loginError, setLoginError] = useState("");

    const [accounts, setAccounts] = useState<AccountResponse[]>([]);
    const [alerts, setAlerts] = useState<RiskAlertResponse[]>([]);
    const [events, setEvents] = useState<FinancialEventResponse[]>([]);

    const [loading, setLoading] = useState(false);
    const [dashboardError, setDashboardError] = useState("");

    const [paymentForm, setPaymentForm] = useState<PaymentForm>(initialPaymentForm);
    const [paymentResult, setPaymentResult] = useState<PaymentAuthorizationResponse | null>(null);

    const token = auth?.accessToken;

    const canViewAlerts = auth?.role === "ANALYST" || auth?.role === "ADMIN";
    const canViewEventLog = auth?.role === "ADMIN";

    const totalBalance = useMemo(
        () => accounts.reduce((sum, account) => sum + Number(account.balance), 0),
        [accounts]
    );

    useEffect(() => {
        if (token) {
            void loadDashboard();
        }
    }, [token]);

    async function handleLogin(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();

        setLoginError("");

        try {
            const response = await apiRequest<LoginResponse>("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: loginEmail,
                    password: loginPassword,
                }),
            });

            localStorage.setItem("paychecker.auth", JSON.stringify(response));
            setAuth(response);
        } catch (error) {
            setLoginError(error instanceof Error ? error.message : "Login failed");
        }
    }

    function handleLogout() {
        localStorage.removeItem("paychecker.auth");
        setAuth(null);
        setAccounts([]);
        setAlerts([]);
        setEvents([]);
        setPaymentResult(null);
    }

    async function loadDashboard() {
        if (!token) return;

        setLoading(true);
        setDashboardError("");

        try {
            const accountsPage = await apiRequest<PageResponse<AccountResponse>>(
                "/api/accounts?page=0&size=5&sort=createdAt,desc",
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

            if (canViewAlerts) {
                const alertsPage = await apiRequest<PageResponse<RiskAlertResponse>>(
                    "/api/alerts?page=0&size=5&sort=createdAt,desc",
                    {},
                    token
                );

                setAlerts(alertsPage.content);
            } else {
                setAlerts([]);
            }

            if (canViewEventLog) {
                const eventsPage = await apiRequest<PageResponse<FinancialEventResponse>>(
                    "/api/event-log?page=0&size=6&sort=createdAt,desc",
                    {},
                    token
                );

                setEvents(eventsPage.content);
            } else {
                setEvents([]);
            }
        } catch (error) {
            setDashboardError(error instanceof Error ? error.message : "Failed to load dashboard");
        } finally {
            setLoading(false);
        }
    }

    async function createDemoAccount() {
        if (!token) return;

        const randomSuffix = Math.floor(Math.random() * 999999999)
            .toString()
            .padStart(9, "0");

        try {
            await apiRequest<AccountResponse>(
                "/api/accounts",
                {
                    method: "POST",
                    body: JSON.stringify({
                        ownerName: "Demo Customer",
                        iban: `PT500777000000000${randomSuffix}`,
                        currency: "EUR",
                        initialBalance: 10000.0,
                        dailyLimit: 10000.0,
                        monthlyLimit: 50000.0,
                    }),
                },
                token
            );

            await loadDashboard();
        } catch (error) {
            setDashboardError(error instanceof Error ? error.message : "Failed to create demo account");
        }
    }

    async function authorizePayment(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!token) return;

        setPaymentResult(null);
        setDashboardError("");

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
            await loadDashboard();
        } catch (error) {
            setDashboardError(error instanceof Error ? error.message : "Payment authorization failed");
        }
    }

    if (!auth) {
        return (
            <main className="login-page">
                <section className="login-card">
                    <div className="brand-mark">
                        <ShieldCheck size={34} />
                    </div>

                    <h1>PayChecker</h1>
                    <p className="muted">
                        Payment authorization, fraud risk scoring and audit logging platform.
                    </p>

                    <form onSubmit={handleLogin} className="login-form">
                        <label>
                            Email
                            <input
                                value={loginEmail}
                                onChange={(event) => setLoginEmail(event.target.value)}
                                placeholder="david@example.com"
                            />
                        </label>

                        <label>
                            Password
                            <input
                                type="password"
                                value={loginPassword}
                                onChange={(event) => setLoginPassword(event.target.value)}
                                placeholder="Password123"
                            />
                        </label>

                        {loginError && <div className="error-box">{loginError}</div>}

                        <button type="submit" className="primary-button">
                            Sign in
                        </button>
                    </form>
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
                    <a className="active">Dashboard</a>
                    <a>Accounts</a>
                    <a>Payments</a>
                    <a>Risk Alerts</a>
                    <a>Event Log</a>
                    <a>Settings</a>
                </nav>

                <div className="sidebar-user">
                    <UserRound size={18} />
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
                        <h1>Risk & Payments Dashboard</h1>
                    </div>

                    <div className="topbar-actions">
                        <button className="ghost-button" onClick={loadDashboard}>
                            <RefreshCw size={16} />
                            Refresh
                        </button>

                        <button className="ghost-button" onClick={handleLogout}>
                            <LogOut size={16} />
                            Logout
                        </button>
                    </div>
                </header>

                {dashboardError && <div className="error-box">{dashboardError}</div>}

                <section className="kpi-grid">
                    <KpiCard
                        title="Accounts"
                        value={accounts.length.toString()}
                        label="Loaded accounts"
                        icon={<CreditCard />}
                        tone="blue"
                    />

                    <KpiCard
                        title="Total Balance"
                        value={`€${totalBalance.toLocaleString("en-US")}`}
                        label="Across loaded accounts"
                        icon={<CheckCircle2 />}
                        tone="green"
                    />

                    <KpiCard
                        title="Risk Alerts"
                        value={canViewAlerts ? alerts.length.toString() : "Restricted"}
                        label={canViewAlerts ? "Visible to analyst/admin" : "Requires analyst role"}
                        icon={<AlertTriangle />}
                        tone="amber"
                    />

                    <KpiCard
                        title="Event Log"
                        value={canViewEventLog ? events.length.toString() : "Restricted"}
                        label={canViewEventLog ? "Admin audit view" : "Requires admin role"}
                        icon={<FileText />}
                        tone="purple"
                    />
                </section>

                <section className="content-grid">
                    <article className="panel payment-panel">
                        <div className="panel-header">
                            <div>
                                <h2>Authorize Payment</h2>
                                <p>Run validation and fraud scoring against a payment request.</p>
                            </div>
                            <span className="pill pill-blue">Live API</span>
                        </div>

                        <form className="payment-form" onSubmit={authorizePayment}>
                            <label>
                                Account ID
                                <input
                                    value={paymentForm.accountId}
                                    onChange={(event) =>
                                        setPaymentForm({ ...paymentForm, accountId: event.target.value })
                                    }
                                    placeholder="1"
                                />
                            </label>

                            <label>
                                Amount
                                <input
                                    value={paymentForm.amount}
                                    onChange={(event) =>
                                        setPaymentForm({ ...paymentForm, amount: event.target.value })
                                    }
                                />
                            </label>

                            <label>
                                Currency
                                <input
                                    value={paymentForm.currency}
                                    onChange={(event) =>
                                        setPaymentForm({ ...paymentForm, currency: event.target.value })
                                    }
                                />
                            </label>

                            <label>
                                Beneficiary IBAN
                                <input
                                    value={paymentForm.beneficiaryIban}
                                    onChange={(event) =>
                                        setPaymentForm({ ...paymentForm, beneficiaryIban: event.target.value })
                                    }
                                />
                            </label>

                            <label>
                                Beneficiary Name
                                <input
                                    value={paymentForm.beneficiaryName}
                                    onChange={(event) =>
                                        setPaymentForm({ ...paymentForm, beneficiaryName: event.target.value })
                                    }
                                />
                            </label>

                            <label>
                                Beneficiary Country
                                <input
                                    value={paymentForm.beneficiaryCountry}
                                    onChange={(event) =>
                                        setPaymentForm({ ...paymentForm, beneficiaryCountry: event.target.value })
                                    }
                                />
                            </label>

                            <button className="primary-button" type="submit">
                                Authorize Payment
                            </button>
                        </form>

                        {paymentResult && (
                            <div className={`decision-card decision-${paymentResult.decision.toLowerCase()}`}>
                                <div>
                                    <p className="muted">Decision</p>
                                    <h3>{paymentResult.decision}</h3>
                                </div>

                                <div>
                                    <p className="muted">Risk Score</p>
                                    <h3>{paymentResult.riskScore}/100</h3>
                                </div>

                                <div className="reason-list">
                                    {paymentResult.reasons.map((reason) => (
                                        <span key={reason}>{reason}</span>
                                    ))}
                                </div>
                            </div>
                        )}
                    </article>

                    <article className="panel">
                        <div className="panel-header">
                            <div>
                                <h2>Accounts</h2>
                                <p>Recently loaded accounts from the API.</p>
                            </div>
                            <button className="small-button" onClick={createDemoAccount}>
                                Add demo account
                            </button>
                        </div>

                        <div className="table-list">
                            {accounts.length === 0 && <p className="empty">No accounts loaded yet.</p>}

                            {accounts.map((account) => (
                                <div className="table-row" key={account.id}>
                                    <div>
                                        <strong>{account.ownerName}</strong>
                                        <span>{account.iban}</span>
                                    </div>
                                    <div className="right">
                                        <strong>€{Number(account.balance).toLocaleString("en-US")}</strong>
                                        <span className="pill pill-green">{account.status}</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </article>
                </section>

                <section className="content-grid lower-grid">
                    <article className="panel">
                        <div className="panel-header">
                            <div>
                                <h2>Risk Alerts</h2>
                                <p>Visible for analyst/admin roles.</p>
                            </div>
                            <span className="pill pill-amber">{auth.role}</span>
                        </div>

                        {!canViewAlerts && <p className="empty">Your current role cannot view risk alerts.</p>}

                        {canViewAlerts && (
                            <div className="table-list">
                                {alerts.length === 0 && <p className="empty">No alerts found.</p>}

                                {alerts.map((alert) => (
                                    <div className="table-row" key={alert.id}>
                                        <div>
                                            <strong>Alert #{alert.id}</strong>
                                            <span>{alert.reasonSummary}</span>
                                        </div>
                                        <div className="right">
                                            <strong>{alert.riskScore}/100</strong>
                                            <span className={`pill ${severityClass(alert.severity)}`}>
                        {alert.severity}
                      </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </article>

                    <article className="panel">
                        <div className="panel-header">
                            <div>
                                <h2>Event Log</h2>
                                <p>Admin-only audit and security events.</p>
                            </div>
                            <Clock3 size={18} />
                        </div>

                        {!canViewEventLog && <p className="empty">Your current role cannot view the event log.</p>}

                        {canViewEventLog && (
                            <div className="event-feed">
                                {events.length === 0 && <p className="empty">No events found.</p>}

                                {events.map((event) => (
                                    <div className="event-item" key={event.id}>
                                        <span className="event-dot" />
                                        <div>
                                            <strong>{event.eventType}</strong>
                                            <p>
                                                {event.entityType} #{event.entityId} · {new Date(event.createdAt).toLocaleString()}
                                            </p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </article>
                </section>

                {loading && <p className="loading">Loading dashboard...</p>}
            </section>
        </main>
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
    icon: React.ReactNode;
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

function severityClass(severity: string) {
    if (severity === "CRITICAL") return "pill-red";
    if (severity === "HIGH") return "pill-amber";
    if (severity === "MEDIUM") return "pill-blue";
    return "pill-green";
}

export default App;