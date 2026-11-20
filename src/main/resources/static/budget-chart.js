export function currentMonth() {
  const today = new Date();
  return today.getFullYear() + "-" + String(today.getMonth() + 1).padStart(2, "0");
}

export async function loadActivity(id, month) {
  const response = await fetch("/api/budgets/" + encodeURIComponent(id) + "/activity?month=" + encodeURIComponent(month), { cache: "no-store" });
  if (!response.ok) throw new Error("Budget activity unavailable");
  return response.json();
}

export function money(amount, currency) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency, currencyDisplay: "code", maximumFractionDigits: 12 }).format(amount);
}

export function renderChart(container, activity, { compact = false } = {}) {
  container.replaceChildren();
  if (!activity.currencies.length) {
    container.textContent = "No limit or activity this month.";
    return;
  }
  for (const totals of activity.currencies) {
    const figure = document.createElement("figure");
    figure.className = "budget-figure";
    const balance = totals.remaining;
    const caption = document.createElement("figcaption");
    function line(text) {
      const p = document.createElement("div");
      p.textContent = text;
      caption.append(p);
    }

    if (balance != null) {
      const available = Number(totals.limit);
      const pie = document.createElement("div");
      pie.className = "budget-pie";
      pie.setAttribute("role", "img");
      pie.setAttribute("aria-label", money(totals.spent, totals.currency) + " spent of " + money(totals.limit, totals.currency));
      const percent = available > 0 ? Math.min(100, 100 * totals.spent / available) : totals.spent > 0 ? 100 : 0;
      pie.style.background = "conic-gradient(#bd3838 " + percent + "%, #419260 0)";
      figure.append(pie);
      line(money(Math.abs(balance), totals.currency) + (balance < 0 ? " over budget" : " left to spend"));
      if (!compact) line("Budget " + money(totals.limit, totals.currency));
    } else {
      line(totals.partialPeriod ? "Limit spans multiple months" : "No limit set");
    }
    if (!compact) line("Spent " + money(totals.spent, totals.currency));
    figure.append(caption);
    container.append(figure);
  }
}
