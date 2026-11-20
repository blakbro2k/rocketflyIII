import { currentMonth, money } from "./budget-chart.js";

export function renderProgress(container, activity, month) {
  const days = new Date(Number(month.slice(0, 4)), Number(month.slice(5)), 0).getDate();
  const today = month === currentMonth() ? new Date().getDate() : 0;
  for (const totals of activity.currencies) {
    if (!(totals.limit > 0)) continue;
    const daily = Array(days + 1).fill(0);
    for (const entry of activity.transactions) {
      if (entry.currency === totals.currency && entry.type === "withdrawal")
        daily[Number(entry.date.slice(-2))] += Number(entry.amount);
    }
    const figure = document.createElement("figure");
    figure.className = "progress-chart";
    const title = document.createElement("figcaption");
    title.textContent = "Cumulative spending · Budget " + money(totals.limit, totals.currency);
    const plot = document.createElement("div");
    plot.className = "day-plot";
    let spent = 0;
    for (let day = 1; day <= days; day++) {
      spent += daily[day];
      const column = document.createElement("div");
      column.className = "day-column";
      if (day === today) column.classList.add("today");
      if (day <= today || month < currentMonth() || daily[day] > 0) {
        const bar = document.createElement("div");
        bar.style.height = Math.min(100, 100 * spent / totals.limit) + "%";
        column.title = "Day " + day + " · " + money(spent, totals.currency);
        column.append(bar);
      }
      const tick = document.createElement("small");
      tick.textContent = day === today ? "Today" : [1, days].includes(day) ? day : "";
      column.append(tick);
      plot.append(column);
    }
    figure.append(title, plot);
    container.append(figure);
  }
}
