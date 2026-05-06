import { useState, FormEvent } from 'react';
import api from '../api/client';

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateCredentialModal({ onClose, onCreated }: Props) {
  const [form, setForm] = useState({ name: '', url: '', username: '', password: '' });
  const [error, setError] = useState('');

  function set(field: string, value: string) {
    setForm(f => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.post('/api/credentials', form);
      onCreated();
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Failed to create credential');
    }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: '#fff', padding: 32, borderRadius: 8, minWidth: 400 }}>
        <h3>New Credential</h3>
        <form onSubmit={handleSubmit}>
          <div><label>Name</label><input value={form.name} onChange={e => set('name', e.target.value)} required /></div>
          <div><label>URL</label><input type="url" value={form.url} onChange={e => set('url', e.target.value)} required /></div>
          <div><label>Username</label><input value={form.username} onChange={e => set('username', e.target.value)} required /></div>
          <div><label>Password</label><input type="password" value={form.password} onChange={e => set('password', e.target.value)} required /></div>
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <div>
            <button type="button" onClick={onClose}>Cancel</button>
            <button type="submit">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
}
