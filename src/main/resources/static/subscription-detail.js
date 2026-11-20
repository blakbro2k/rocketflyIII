import { monthPicker } from "./settings.js";
import { money } from "./budget-chart.js";

const month = monthPicker.value;
const id = new URLSearchParams(location.search).get("id");
document.querySelector("main a").href = "/?month=" + month;
document.querySelector("#period").textContent = new Date(month + "-01T12:00:00")
  .toLocaleDateString(undefined, { month: "long", year: "numeric" });

async function loadSubscription() {
  const status = document.querySelector("#status");
  try {
    if (!/^\d+$/.test(id)) throw new Error();
    const response = await fetch("/api/subscriptions/" + id + "?month=" + month, { cache: "no-store" });
    if (!response.ok) throw new Error();
    const bill = await response.json();
    if (typeof bill.name !== "string" || typeof bill.active !== "boolean") throw new Error();
    document.querySelector("#name").textContent = bill.name;
    document.title = bill.name + " · RocketFlyIII";
    const details = [bill.active ? "Active" : "Inactive"];
    if (bill.repeat_freq) details.push(bill.repeat_freq);
    if (bill.amount_min != null && bill.amount_max != null && bill.currency_code) {
      const low = money(bill.amount_min, bill.currency_code);
      const high = money(bill.amount_max, bill.currency_code);
      details.push(low === high ? low : low + " – " + high);
    }
    document.querySelector("#definition").textContent = details.join(" · ");
    if (!Array.isArray(bill.paid_dates)) {
      status.textContent = "Payment history unavailable.";
      return;
    }
    const rows = bill.paid_dates.filter(payment => payment.date?.startsWith(month + "-"))
      .sort((a, b) => b.date.localeCompare(a.date));
    const table = document.querySelector("#payments");
    for (const payment of rows) {
      const row = table.tBodies[0].insertRow();
      row.insertCell().textContent = payment.date.slice(0, 10);
      const currency = payment.currency_code || bill.currency_code;
      row.insertCell().textContent = payment.amount != null && currency ? money(payment.amount, currency) : "—";
    }
    table.hidden = rows.length === 0;
    status.textContent = rows.length ? "" : "No linked payments this month.";
  } catch {
    status.textContent = "Couldn’t load this subscription. Refresh to try again.";
  }
}
loadSubscription();
