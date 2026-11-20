import { loadActivity, renderChart } from "./budget-chart.js";
const budgetList = document.querySelector("#budget-list");
const budgetStatus = document.querySelector("#budget-status");
const budgetRefresh = document.querySelector("#budget-refresh");
const navigation = document.querySelector("#budget-navigation");
const previous = document.querySelector("#budget-prev");
const next = document.querySelector("#budget-next");

function updateBudgetNavigation() {
  const end = budgetList.scrollWidth - budgetList.clientWidth;
  navigation.hidden = budgetList.hidden || end <= 1;
  previous.disabled = budgetList.scrollLeft <= 1;
  next.disabled = budgetList.scrollLeft >= end - 1;
}

function moveBudgets(direction) {
  const card = budgetList.firstElementChild;
  if (card) {
    const gap = parseFloat(getComputedStyle(budgetList).columnGap) || 0;
    budgetList.scrollBy({ left: direction * (card.offsetWidth + gap) });
  }
}

import { monthPicker } from "./settings.js";

async function loadBudgets() {
  const month = monthPicker.value;
  monthPicker.disabled = true;
  budgetRefresh.disabled = true;
  budgetStatus.textContent = "Loading budgets from Firefly III…";
  try {
    const response = await fetch("/api/budgets", { cache: "no-store" });
    if (!response.ok) throw new Error("Budget request failed");
    const budgets = await response.json();
    const cards = document.createDocumentFragment();
    for (const { id, attributes } of budgets) {
      if (typeof attributes.name !== "string" || typeof attributes.active !== "boolean") {
        throw new Error("Invalid budget");
      }
      const card = document.createElement("li");
      const name = document.createElement("strong");
      name.textContent = attributes.name;
      const link = document.createElement("a");
      link.className = "budget-link";
      link.href = "budget.html?" + new URLSearchParams({ id, month });
      link.append(name);
      const chart = document.createElement("div");
      chart.textContent = "Loading chart…";
      card.append(link, chart);
      loadActivity(id, month).then(activity => {
        renderChart(chart, activity, { compact: true });
      }).catch(() => { chart.textContent = "Chart unavailable."; });
      cards.append(card);
    }
    budgetList.replaceChildren(cards);
    budgetList.hidden = budgets.length === 0;
    budgetStatus.textContent = budgets.length
      ? budgets.length + (budgets.length === 1 ? " budget" : " budgets") + " from Firefly III"
      : "No budgets in Firefly III.";
    updateBudgetNavigation();
  } catch {
    budgetStatus.textContent = budgetList.childElementCount
      ? "Refresh failed. Showing the previous budgets."
      : "Couldn’t load budgets from Firefly III. Select Refresh to try again.";
  } finally {
    monthPicker.disabled = false;
    budgetRefresh.disabled = false;
  }
}

previous.addEventListener("click", () => moveBudgets(-1));
next.addEventListener("click", () => moveBudgets(1));
budgetRefresh.addEventListener("click", loadBudgets);
budgetList.addEventListener("scroll", updateBudgetNavigation);
new ResizeObserver(updateBudgetNavigation).observe(budgetList);
loadBudgets();
window.addEventListener("pageshow", event => {
  if (event.persisted) loadBudgets();
});
