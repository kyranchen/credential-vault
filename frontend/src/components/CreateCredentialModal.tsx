import { useState, FormEvent } from 'react';
import api from '../api/client';

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateCredentialModal({ onClose, onCreated }: Props) {
  const [form, setForm] = useState({ name: '', url: '', username: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function set(field: string, value: string) {
    setForm(f => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/api/credentials', form);
      onCreated();
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Failed to create credential');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={modal.overlay} onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div style={modal.panel}>
        {/* Header */}
        <div style={modal.header}>
          <h2 style={modal.title}>New Credential</h2>
          <button style={modal.closeBtn} onClick={onClose} aria-label="Close">✕</button>
        </div>

        <form onSubmit={handleSubmit} style={modal.form}>
          <Field label="Name" placeholder="Company Twitter">
            <input className="input" value={form.name} onChange={e => set('name', e.target.value)} required />
          </Field>

          <Field label="URL" placeholder="https://twitter.com">
            <input className="input" type="url" value={form.url} onChange={e => set('url', e.target.value)} required />
          </Field>

          <Field label="Username / Email" placeholder="acme_corp">
            <input className="input" value={form.username} onChange={e => set('username', e.target.value)} required />
          </Field>

          <Field label="Password">
            <div style={{ position: 'relative' }}>
              <input
                className="input"
                type={showPassword ? 'text' : 'password'}
                value={form.password}
                onChange={e => set('password', e.target.value)}
                style={{ paddingRight: 44 }}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(s => !s)}
                style={modal.eyeBtn}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </Field>

          {error && <p className="error-banner">{error}</p>}

          <div style={modal.actions}>
            <button type="button" className="btn-ghost" onClick={onClose}>Cancel</button>
            <button
              type="submit"
              className="btn-primary"
              style={{ width: 'auto', padding: '10px 24px' }}
              disabled={loading}
            >
              {loading ? 'Saving…' : 'Save credential'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function Field({ label, placeholder, children }: {
  label: string;
  placeholder?: string;
  children: React.ReactNode;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <label className="label">{label}</label>
      {children}
    </div>
  );
}

const modal: Record<string, React.CSSProperties> = {
  overlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0, 0, 0, 0.6)',
    backdropFilter: 'blur(4px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 50,
    padding: 24,
  },
  panel: {
    background: '#1e293b',
    border: '1px solid #334155',
    borderRadius: 16,
    width: '100%',
    maxWidth: 460,
    boxShadow: '0 25px 60px rgba(0,0,0,0.5)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px 24px 0',
  },
  title: { fontSize: 18, fontWeight: 700, color: '#f1f5f9' },
  closeBtn: {
    background: 'none',
    border: 'none',
    color: '#64748b',
    fontSize: 18,
    cursor: 'pointer',
    lineHeight: 1,
    padding: 4,
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: 18,
    padding: '20px 24px 24px',
  },
  eyeBtn: {
    position: 'absolute',
    right: 10,
    top: '50%',
    transform: 'translateY(-50%)',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: 16,
    lineHeight: 1,
    padding: 2,
  },
  actions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: 10,
    marginTop: 4,
  },
};
