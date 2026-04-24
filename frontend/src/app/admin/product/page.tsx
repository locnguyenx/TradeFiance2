import { SystemAdminSettings } from '../../../components/SystemAdminSettings';
import { GlobalShell } from '../../../components/GlobalShell';

export default function ProductAdminPage() {
  return (
    <GlobalShell>
      <SystemAdminSettings activePanel="product" />
    </GlobalShell>
  );
}
