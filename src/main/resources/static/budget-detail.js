import { currentMonth, loadActivity, money, renderChart, transferToggle } from "./budget-chart.js";

const params = new URLSearchParams(location.search);
const id = params.get("id");
const month = params.get("month") || currentMonth();
const chart = document.querySelector("#chart");
const transactionStatus = document.querySelector("#transaction-status");

async function showBudget() {
  if (!/^\d+$/.test(id) || !/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) {
    chart.textContent = "Invalid budget link.";
    return;
  }
  document.querySelector("#name").textContent = "Budget " + id;
  document.querySelector("#period").textContent = new Intl.DateTimeFormat(undefined, { month: "long", year: "numeric" }).format(new Date(month + "-01T12:00:00"));
  const toggle = transferToggle(id);
  document.querySelector("#preference").append(toggle.label);
  try {
    const activity = await loadActivity(id, month);
    const draw = () => renderChart(chart, activity, toggle.input.checked);
    draw();
    toggle.input.addEventListener("change", draw);
    const table = document.querySelector("#transactions");
    for (const entry of activity.transactions) {
      const row = table.tBodies[0].insertRow();
      for (const value of [entry.date, entry.description, entry.type, money(entry.amount, entry.currency)]) {
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
