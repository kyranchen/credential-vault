import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import CredentialCard from '../components/CredentialCard';
import CreateCredentialModal from '../components/CreateCredentialModal';

interface Credential {
  id: string;
  name: string;
  url: string;
  username: string;
  createdAt: string;
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [loading, setLoading] = useState(true);

  async function loadCredentials() {
    try {
      const { data } = await api.get<Credential[]>('/api/credentials');
      setCredentials(data);
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    localStorage.removeItem('token');
    navigate('/login');
  }

  useEffect(() => { loadCredentials(); }, []);

  return (
    <div style={{ minHeight: '100vh', background: '#0f172a' }}>
      {/* Navbar */}
      <nav style={nav.bar}>
        <div style={nav.inner}>
          <div style={nav.brand}>
            <span style={nav.logo}>🔐</span>
            <span style={nav.name}>Credential Vault</span>
          </div>
          <button className="btn-ghost" onClick={handleLogout}>Sign out</button>
        </div>
      </nav>

      {/* Content */}
      <main style={main.wrapper}>
        {/* Header row */}
        <div style={main.header}>
          <div>
            <h1 style={main.title}>Credentials</h1>
            <p style={main.subtitle}>
              {credentials.length} credential{credentials.length !== 1 ? 's' : ''} stored
            </p>
          </div>
          <button
            className="btn-primary"
            style={{ width: 'auto', padding: '10px 20px' }}
            onClick={() => setShowCreate(true)}
          >
            + Add Credential
          </button>
        </div>

        {/* List */}
        {loading ? (
          <p style={{ color: '#475569', textAlign: 'center', marginTop: 60 }}>Loading…</p>
        ) : credentials.length === 0 ? (
          <div style={empty.box}>
            <div style={empty.icon}>🔑</div>
            <p style={empty.text}>No credentials yet</p>
            <p style={empty.sub}>Add your first credential to get started</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {credentials.map(cred => (
              <CredentialCard key={cred.id} credential={cred} onDeleted={loadCredentials} />
            ))}
          </div>
        )}
      </main>

      {showCreate && (
        <CreateCredentialModal
          onClose={() => setShowCreate(false)}
          onCreated={() => { setShowCreate(false); loadCredentials(); }}
        />
      )}
    </div>
  );
}

const nav: Record<string, React.CSSProperties> = {
  bar: {
    background: '#1e293b',
    borderBottom: '1px solid #334155',
    position: 'sticky',
    top: 0,
    zIndex: 10,
  },
  inner: {
    maxWidth: 900,
    margin: '0 auto',
    padding: '14px 24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brand: { display: 'flex', alignItems: 'center', gap: 10 },
  logo: { fontSize: 20 },
  name: { fontSize: 16, fontWeight: 700, color: '#f1f5f9' },
};

const main: Record<string, React.CSSProperties> = {
  wrapper: { maxWidth: 900, margin: '0 auto', padding: '40px 24px' },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 32,
  },
  title: { fontSize: 24, fontWeight: 700, color: '#f1f5f9', marginBottom: 4 },
  subtitle: { fontSize: 14, color: '#64748b' },
};

const empty: Record<string, React.CSSProperties> = {
  box: {
    textAlign: 'center',
    padding: '80px 24px',
    background: '#1e293b',
    border: '1px dashed #334155',
    borderRadius: 12,
  },
  icon: { fontSize: 40, marginBottom: 16 },
  text: { fontSize: 16, fontWeight: 600, color: '#94a3b8', marginBottom: 6 },
  sub: { fontSize: 14, color: '#475569' },
};
