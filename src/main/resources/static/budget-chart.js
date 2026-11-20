export function currentMonth() {
  const today = new Date();
  return today.getFullYear() + "-" + String(today.getMonth() + 1).padStart(2, "0");
}

export function transferToggle(id) {
  const label = document.createElement("label");
  label.className = "transfer-toggle";
  const input = document.createElement("input");
  input.type = "checkbox";
  const key = "budget-transfer-income:" + id;
  try { input.checked = sessionStorage.getItem(key) === "true"; } catch { /* Storage is optional. */ }
  input.addEventListener("change", () => {
    try { sessionStorage.setItem(key, String(input.checked)); } catch { /* Keep the local selection. */ }
  });
  label.append(input, " Transfers as Income");
  return { label, input };
}

export async function loadActivity(id, month) {
  const response = await fetch("/api/budgets/" + encodeURIComponent(id) + "/activity?month=" + encodeURIComponent(month), { cache: "no-store" });
  if (!response.ok) throw new Error("Budget activity unavailable");
  return response.json();
}

export function money(amount, currency) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency, currencyDisplay: "code", maximumFractionDigits: 12 }).format(amount);
}

export function renderChart(container, activity, includeTransfers) {
  container.replaceChildren();
  if (!activity.currencies.length) {
    container.textContent = "No limit or activity this month.";
    return;
  }
  for (const totals of activity.currencies) {
    const figure = document.createElement("figure");
    figure.className = "budget-figure";
    const balance = includeTransfers ? totals.remainingWithTransfers : totals.remaining;
    const caption = document.createElement("figcaption");
    function line(text) {
      const p = document.createElement("div");
      p.textContent = text;
      caption.append(p);
    }
    line("Spent " + money(totals.spent, totals.currency));
    if (balance != null) {
      const remaining = Math.max(0, balance);
      const slices = totals.spent + remaining;
      const pie = document.createElement("div");
      pie.className = "budget-pie";
      pie.setAttribute("aria-hidden", "true");
      if (slices > 0) pie.style.background = "conic-gradient(#245b46 " + (100 * totals.spent / slices) + "%, #cadfce 0)";
      figure.append(pie);
      line((balance < 0 ? "Over by " : "Remaining ") + money(Math.abs(balance), totals.currency));
      line("Limit " + money(totals.limit, totals.currency));
    } else {
      line(totals.partialPeriod ? "Limit spans multiple months" : "No limit set");
    }
    if (includeTransfers) line("Transfer income " + money(totals.transfers, totals.currency));
    figure.append(caption);
    container.append(figure);
  }
}
