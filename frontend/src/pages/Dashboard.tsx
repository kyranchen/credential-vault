import { useEffect, useState } from 'react';
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
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [showCreate, setShowCreate] = useState(false);

  async function loadCredentials() {
    const { data } = await api.get<Credential[]>('/api/credentials');
    setCredentials(data);
  }

  useEffect(() => { loadCredentials(); }, []);

  return (
    <div style={{ maxWidth: 900, margin: '40px auto', padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Credentials</h2>
        <button onClick={() => setShowCreate(true)}>+ Add Credential</button>
      </div>

      <div>
        {credentials.map(cred => (
          <CredentialCard key={cred.id} credential={cred} onDeleted={loadCredentials} />
        ))}
        {credentials.length === 0 && <p>No credentials yet.</p>}
      </div>

      {showCreate && (
        <CreateCredentialModal
          onClose={() => setShowCreate(false)}
          onCreated={() => { setShowCreate(false); loadCredentials(); }}
        />
      )}
    </div>
  );
}
