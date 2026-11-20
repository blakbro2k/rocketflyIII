import { currentMonth, money } from "./budget-chart.js";

export function renderProgress(container, totals, transactions, month) {
  const days = new Date(Number(month.slice(0, 4)), Number(month.slice(5)), 0).getDate();
  const today = month === currentMonth() ? new Date().getDate() : 0;
  let lastDay = month < currentMonth() ? days : today;
  const daily = Array(days + 1).fill(0);
  for (const entry of transactions) {
    if (entry.currency !== totals.currency || entry.type !== "withdrawal") continue;
    const day = Number(entry.date.slice(-2));
    daily[day] += Number(entry.amount);
    lastDay = Math.max(day, lastDay);
  }
  const figure = document.createElement("figure");
  figure.className = "progress-chart";
  const caption = document.createElement("figcaption");
  caption.textContent = "Cumulative spending · " + totals.currency;
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 500 132");
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", "Cumulative spending: " + money(totals.spent, totals.currency));
  const maximum = Math.max(totals.limit || 0, totals.spent, 1);
  const points = ["2,130"];
  let spent = 0;
  for (let day = 1; day <= lastDay; day++) {
    spent += daily[day];
    points.push((2 + (day - 1) / (days - 1) * 496) + "," + (130 - spent / maximum * 126));
  }
  const line = document.createElementNS(svg.namespaceURI, "polyline");
  line.setAttribute("points", points.join(" "));
  svg.append(line);
  const axis = document.createElement("div");
  axis.className = "budget-axis";
  axis.append("Day 1", Object.assign(document.createElement("span"), { textContent: "Day " + days }));
  figure.append(caption, document.createTextNode(money(maximum, totals.currency)), svg, axis);
  container.append(figure);

  if (!(totals.limit > 0)) return;
  const pace = document.createElement("figure");
  pace.className = "budget-pace";
  const label = document.createElement("figcaption");
  label.textContent = money(totals.spent, totals.currency) + " spent of " + money(totals.limit, totals.currency);
  const track = document.createElement("div");
  track.className = "pace-track";
  const fill = document.createElement("div");
  fill.style.width = Math.min(100, 100 * totals.spent / totals.limit) + "%";
  track.append(fill);
  if (today) {
    const marker = document.createElement("span");
    marker.className = "pace-today";
    marker.style.left = 100 * today / days + "%";
    marker.textContent = "Today";
    marker.title = "Day " + today + " of " + days;
    track.append(marker);
  }
  pace.append(label, track);
  container.append(pace);
}
