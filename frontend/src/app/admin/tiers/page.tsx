import { SystemAdminSettings } from '../../../components/SystemAdminSettings';
import { GlobalShell } from '../../../components/GlobalShell';

export default function TierAdminPage() {
  return (
    <GlobalShell>
      <SystemAdminSettings activePanel="authority" />
    </GlobalShell>
  );
}
