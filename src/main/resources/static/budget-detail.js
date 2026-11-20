import { loadActivity, money, renderChart } from "./budget-chart.js";

const params = new URLSearchParams(location.search);
const id = params.get("id");
import { monthPicker } from "./settings.js";
import { renderProgress } from "./progress.js";
const month = monthPicker.value;
document.querySelector(".budget-detail a").href = "/?month=" + month;
const chart = document.querySelector("#chart");
const transactionStatus = document.querySelector("#transaction-status");

async function showBudget() {
  if (!/^\d+$/.test(id) || !/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) {
    chart.textContent = "Invalid budget link.";
    return;
  }
  document.querySelector("#name").textContent = "Budget " + id;
  document.querySelector("#period").textContent = new Intl.DateTimeFormat(undefined, { month: "long", year: "numeric" }).format(new Date(month + "-01T12:00:00"));
  try {
    const activity = await loadActivity(id, month);
    renderChart(chart, activity);
    activity.currencies.forEach((totals, index) => {
      const pair = document.createElement("section");
      pair.className = "budget-plots";
      const pie = chart.querySelectorAll(".budget-figure")[index];
      pie.replaceWith(pair);
      pair.append(pie);
      renderProgress(pair, totals, activity.transactions, month);
    });
    const table = document.querySelector("#transactions");
    for (const entry of activity.transactions) {
      const row = table.tBodies[0].insertRow();
      for (const value of [entry.description, money(entry.type === "withdrawal" ? -entry.amount : entry.amount, entry.currency), entry.runningBalance == null ? "—" : money(entry.runningBalance, entry.currency), entry.date, entry.source || "—", entry.destination || "—", entry.category || "—"]) {
        row.insertCell().textContent = value;
      }
    }
    table.hidden = activity.transactions.length === 0;
    if (table.hidden) transactionStatus.textContent = "No transactions this month.";
  } catch {
    chart.textContent = "Budget activity unavailable. Reload to try again.";
    transactionStatus.textContent = "Transactions unavailable.";
  }
}

showBudget();
if (/^\d+$/.test(id)) {
  fetch("/api/budgets").then(r => r.ok ? r.json() : []).then(budgets => {
    const budget = budgets.find(b => String(b.id) === id);
    if (budget) document.querySelector("#name").textContent = budget.attributes.name;
  }).catch(() => { /* The ID still identifies this budget. */ });
}
