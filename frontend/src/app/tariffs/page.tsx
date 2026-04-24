import { SystemAdminSettings } from '../../components/SystemAdminSettings';
import { GlobalShell } from '../../components/GlobalShell';

export default function TariffsPage() {
  return (
    <GlobalShell>
      <SystemAdminSettings activePanel="tariff" />
    </GlobalShell>
  );
}
