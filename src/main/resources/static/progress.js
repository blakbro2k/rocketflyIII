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
  svg.setAttribute("viewBox", "0 0 460 180");
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", "Cumulative spending: " + money(totals.spent, totals.currency));
  const maximum = Math.max(totals.limit || 0, totals.spent, 1);
  const magnitude = 10 ** Math.floor(Math.log10(maximum / 4));
  const step = [1, 2, 2.5, 5, 10].find(n => n * magnitude >= maximum / 4) * magnitude;
  const intervals = Math.ceil(maximum / step);
  const ceiling = intervals * step;
  const formatter = new Intl.NumberFormat(undefined, {
    notation: ceiling >= 100000 ? "compact" : "standard", maximumSignificantDigits: 4
  });
  const x = day => 64 + (day - 1) / (days - 1) * 382;
  const y = amount => 144 - amount / ceiling * 132;

  function element(tag, attributes, text) {
    const node = document.createElementNS(svg.namespaceURI, tag);
    for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, value);
    if (text != null) node.textContent = text;
    svg.append(node);
  }

  for (let i = 0; i <= intervals; i++) {
    const amount = i * step;
    const height = y(amount);
    element("line", { x1: 59, x2: x(days), y1: height, y2: height });
    element("text", { x: 54, y: height + 4, "text-anchor": "end" }, formatter.format(amount));
  }
  element("line", { x1: x(1), x2: x(1), y1: y(ceiling), y2: y(0) });
  for (let day = 1; day <= days; day++) {
    const labeled = [1, 7, 14, 21, days].includes(day);
    element("line", { x1: x(day), x2: x(day), y1: 144, y2: labeled ? 151 : 147 });
    if (labeled) element("text", { x: x(day), y: 168, "text-anchor": "middle" }, day);
  }
  const points = [];
  let spent = 0;
  for (let day = 1; day <= lastDay; day++) {
    spent += daily[day];
    points.push(x(day) + "," + y(spent));
  }
  element("polyline", { points: points.join(" ") });
  if (lastDay === 1) element("circle", { cx: x(1), cy: y(spent), r: 2, fill: "#ac3434" });
  figure.append(caption, svg);
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
