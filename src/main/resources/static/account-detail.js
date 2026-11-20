import { monthPicker } from "./settings.js";
import { currentMonth, money } from "./budget-chart.js";

const id = new URLSearchParams(location.search).get("id");
const month = monthPicker.value;
const toggle = document.querySelector("#transfers");
const preference = "account-transfer-income:" + id;
const colors = ["#306682", "#437958", "#99533c", "#72609b", "#a77933", "#307b77"];
document.querySelector("main a").href = "/?month=" + month;
document.querySelector("#period").textContent = new Date(month + "-01T12:00:00")
  .toLocaleDateString(undefined, { month: "long", year: "numeric" });

function drawBalance(account) {
  const figure = document.querySelector("#balance");
  const caption = document.createElement("figcaption");
  caption.textContent = "Month-end balance · " + money(account.closingBalance, account.currency);
  const values = account.dailyBalances.map(Number);
  const low = Math.min(...values);
  const high = Math.max(...values);
  const padding = (high - low) * .08 || 1;
  const min = low - padding;
  const max = high + padding;
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 760 206");
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", "Daily balance for " + month + ", " + caption.textContent);
  const x = day => 105 + (day - 1) * 630 / (values.length - 1);
  const y = value => 173 - 150 * (value - min) / (max - min);
  function mark(tag, attributes, label) {
    const node = document.createElementNS(svg.namespaceURI, tag);
    for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, value);
    if (label != null) node.textContent = label;
    svg.append(node);
    return node;
  }
  for (let i = 0; i <= 4; i++) {
    const value = min + i * (max - min) / 4;
    mark("line", { x1: 104, x2: 735, y1: y(value), y2: y(value), stroke: "#d7dfd8" });
    mark("text", { x: 98, y: y(value) + 4, "text-anchor": "end" },
      value.toLocaleString(undefined, { maximumFractionDigits: 2 }));
  }
  mark("polyline", { points: values.map((value, i) => x(i + 1) + "," + y(value)).join(" "),
    fill: "none", stroke: colors[0], "stroke-width": 2.5 });
  for (const day of [1, 7, 14, 21, values.length]) {
    mark("text", { x: x(day), y: 196, "text-anchor": "middle" }, day);
  }
  if (month === currentMonth()) {
    const pos = x(new Date().getDate());
    mark("line", { x1: pos, x2: pos, y1: 19, y2: 174, stroke: "#99533c", "stroke-dasharray": "4 3" });
    mark("text", { x: pos, y: 14, "text-anchor": "middle" }, "Today");
  }
  figure.append(caption, svg);
}

function breakdown(selector, rows, key, currency) {
  const target = document.querySelector(selector);
  target.replaceChildren();
  const sums = new Map();
  for (const row of rows) {
    const label = row[key] || (key === "budget" ? "No budget" : "Uncategorized");
    sums.set(label, (sums.get(label) || 0) + Math.abs(row.amount));
  }
  const entries = [...sums].filter(([, amount]) => amount > 0).sort((a, b) => b[1] - a[1]);
  if (!entries.length) {
    target.textContent = "No activity this month.";
    return;
  }
  const total = entries.reduce((sum, [, amount]) => sum + amount, 0);
  const legend = document.createElement("ul");
  const slices = [];
  let start = 0;
  entries.forEach(([label, amount], index) => {
    const color = colors[index] || "hsl(" + (index * 137.5 % 360) + " 42% 43%)";
    const end = start + amount / total * 100;
    slices.push(color + " " + start + "% " + end + "%");
    start = end;
    const item = document.createElement("li");
    item.style.setProperty("--slice", color);
    item.textContent = label + " · " + money(amount, currency);
    legend.append(item);
  });
  const pie = document.createElement("div");
  pie.className = "account-pie";
  pie.setAttribute("aria-hidden", "true");
  pie.style.background = "conic-gradient(" + slices.join(",") + ")";
  target.append(pie, legend);
}

async function showAccount() {
  const status = document.querySelector("#status");
  try {
    if (!/^\d+$/.test(id)) throw new Error();
    const response = await fetch("/api/accounts/" + id + "/activity?month=" + month, { cache: "no-store" });
    if (!response.ok) throw new Error();
    const account = await response.json();
    document.querySelector("h1").textContent = account.name;
    document.title = account.name + " · RocketFlyIII";
    drawBalance(account);
    const expenses = account.transactions.filter(row => row.type === "withdrawal" && row.amount < 0);
    breakdown("#expenses", expenses, "category", account.currency);
    breakdown("#budgets", expenses, "budget", account.currency);
    try { toggle.checked = sessionStorage.getItem(preference) === "true"; } catch {}
    function income() {
      breakdown("#income", account.transactions.filter(row => row.amount > 0 &&
        (row.type === "deposit" || (toggle.checked && row.type === "transfer"))), "category", account.currency);
    }
    income();
    toggle.onchange = () => {
      try { sessionStorage.setItem(preference, String(toggle.checked)); } catch {}
      income();
    };
    const table = document.querySelector("#transactions");
    for (const row of account.transactions) {
      const tr = table.tBodies[0].insertRow();
      for (const value of [row.description, money(row.amount, account.currency), money(row.balance, account.currency),
        row.date, row.source, row.destination, row.category, row.budget]) tr.insertCell().textContent = value || "—";
    }
    table.hidden = !account.transactions.length;
    if (table.hidden) document.querySelector("#transaction-status").textContent = "No transactions this month.";
    document.querySelector("#report").hidden = false;
    status.hidden = true;
  } catch {
    status.textContent = "Couldn’t load this account. Check the account link or refresh to try again.";
  }
}
showAccount();
