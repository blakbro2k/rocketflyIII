import { amount, dateLabel, paymentStatus, repeats } from "./subscription-format.js";
import { monthPicker } from "./settings.js";

const subscriptionTable = document.querySelector("#subscription-table");
const subscriptionStatus = document.querySelector("#subscription-status");
const subscriptionRefresh = document.querySelector("#subscription-refresh");

async function loadSubscriptions() {
  subscriptionRefresh.disabled = true;
  subscriptionStatus.textContent = "Loading subscriptions from Firefly III…";
  try {
    const response = await fetch("/api/subscriptions?month=" + monthPicker.value, { cache: "no-store" });
    if (!response.ok) throw new Error("Subscription request failed");
    const subscriptions = await response.json();
    if (!Array.isArray(subscriptions)) throw new Error("Invalid subscription list");

    const rows = document.createElement("tbody");
    for (const { id, attributes } of subscriptions) {
      if (typeof attributes.name !== "string" || typeof attributes.active !== "boolean") {
        throw new Error("Invalid subscription");
      }
      const row = rows.insertRow();
      const link = document.createElement("a");
      link.href = "subscription.html?" + new URLSearchParams({ id, month: monthPicker.value });
      link.textContent = attributes.name;
      row.insertCell().append(link);
      row.insertCell().textContent = attributes.active ? "Active" : "Inactive";
      const price = amount(attributes.amount_avg, attributes.currency_code);
      row.insertCell().textContent = price !== "—" && Number(attributes.amount_min) !== Number(attributes.amount_max) ? "≈ " + price : price;
      row.insertCell().textContent = paymentStatus(attributes, monthPicker.value);
      row.insertCell().textContent = dateLabel(attributes.next_expected_match);
      row.insertCell().textContent = repeats(attributes);
    }
    subscriptionTable.tBodies[0].replaceWith(rows);
    subscriptionTable.hidden = subscriptions.length === 0;
    subscriptionStatus.textContent = subscriptions.length
      ? subscriptions.length + (subscriptions.length === 1 ? " subscription" : " subscriptions") + " from Firefly III"
      : "No subscriptions in Firefly III.";
  } catch {
    subscriptionStatus.textContent = subscriptionTable.hidden
      ? "Couldn’t load subscriptions from Firefly III. Select Refresh to try again."
      : "Refresh failed. Showing the previous subscriptions.";
  } finally {
    subscriptionRefresh.disabled = false;
  }
}

subscriptionRefresh.addEventListener("click", loadSubscriptions);
loadSubscriptions();
