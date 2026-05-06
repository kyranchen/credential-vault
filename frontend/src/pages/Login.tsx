import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

type Tab = 'login' | 'register';

export default function Login() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [orgName, setOrgName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (tab === 'login') {
        const { data } = await api.post('/api/auth/login', { email, password });
        localStorage.setItem('token', data.token);
      } else {
        const { data } = await api.post('/api/auth/register', {
          email,
          password,
          organizationName: orgName,
        });
        localStorage.setItem('token', data.token);
      }
      navigate('/');
    } catch (err: any) {
      const msg = err.response?.data?.error;
      setError(msg ?? (tab === 'login' ? 'Invalid email or password' : 'Registration failed'));
    } finally {
      setLoading(false);
    }
  }

  function switchTab(next: Tab) {
    setTab(next);
    setError('');
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        {/* Header */}
        <div style={styles.header}>
          <div style={styles.logo}>🔐</div>
          <h1 style={styles.title}>Credential Vault</h1>
          <p style={styles.subtitle}>Secure shared credentials for your team</p>
        </div>

        {/* Tab switcher */}
        <div style={styles.tabs}>
          <button
            style={{ ...styles.tab, ...(tab === 'login' ? styles.tabActive : {}) }}
            onClick={() => switchTab('login')}
            type="button"
          >
            Sign in
          </button>
          <button
            style={{ ...styles.tab, ...(tab === 'register' ? styles.tabActive : {}) }}
            onClick={() => switchTab('register')}
            type="button"
          >
            Create account
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} style={styles.form}>
          {tab === 'register' && (
            <Field
              label="Organization name"
              type="text"
              value={orgName}
              onChange={setOrgName}
              placeholder="Acme Corp"
              required
            />
          )}

          <Field
            label="Email"
            type="email"
            value={email}
            onChange={setEmail}
            placeholder="you@company.com"
            required
          />

          <Field
            label="Password"
            type="password"
            value={password}
            onChange={setPassword}
            placeholder={tab === 'register' ? 'At least 8 characters' : ''}
            required
          />

          {error && <p style={styles.error}>{error}</p>}

          <button type="submit" disabled={loading} style={styles.submit}>
            {loading ? 'Please wait…' : tab === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  );
}

function Field({
  label,
  type,
  value,
  onChange,
  placeholder,
  required,
}: {
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <div style={styles.field}>
      <label style={styles.label}>{label}</label>
      <input
        style={styles.input}
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        autoComplete={type === 'password' ? 'current-password' : type === 'email' ? 'email' : 'off'}
      />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#f1f5f9',
    fontFamily: 'system-ui, -apple-system, sans-serif',
  },
  card: {
    background: '#ffffff',
    borderRadius: 12,
    boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
    padding: '40px 36px',
    width: '100%',
    maxWidth: 420,
  },
  header: {
    textAlign: 'center',
    marginBottom: 28,
  },
  logo: {
    fontSize: 36,
    marginBottom: 8,
  },
  title: {
    margin: '0 0 4px',
    fontSize: 22,
    fontWeight: 700,
    color: '#0f172a',
  },
  subtitle: {
    margin: 0,
    fontSize: 14,
    color: '#64748b',
  },
  tabs: {
    display: 'flex',
    borderBottom: '1px solid #e2e8f0',
    marginBottom: 24,
  },
  tab: {
    flex: 1,
    padding: '10px 0',
    background: 'none',
    border: 'none',
    borderBottom: '2px solid transparent',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 500,
    color: '#64748b',
    marginBottom: -1,
  },
  tabActive: {
    borderBottomColor: '#2563eb',
    color: '#2563eb',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: 16,
  },
  field: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: 500,
    color: '#374151',
  },
  input: {
    padding: '10px 12px',
    border: '1px solid #d1d5db',
    borderRadius: 6,
    fontSize: 14,
    outline: 'none',
    transition: 'border-color 0.15s',
  },
  error: {
    margin: 0,
    fontSize: 13,
    color: '#dc2626',
    background: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: 6,
    padding: '8px 12px',
  },
  submit: {
    marginTop: 4,
    padding: '11px 0',
    background: '#2563eb',
    color: '#ffffff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    transition: 'background 0.15s',
  },
};
