import { TelemetryPanel } from "@/features/overview/telemetry";
import { CatalogScreen } from "@/features/overview/catalog-screen";
export default function Page() {
  return (
    <div className="space-y-7">
      <CatalogScreen knowledge />
      <TelemetryPanel detailed />
    </div>
  );
}
