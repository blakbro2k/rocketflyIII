import { monthPicker } from "./settings.js";
import { amount, dateLabel, repeats } from "./subscription-format.js";

const month = monthPicker.value;
const id = new URLSearchParams(location.search).get("id");
document.querySelector("main a").href = "/?month=" + month;
document.querySelector("#period").textContent = new Date(month + "-01T12:00:00")
  .toLocaleDateString(undefined, { month: "long", year: "numeric" });

async function loadSubscription() {
  const status = document.querySelector("#status");
  try {
    if (!/^\d+$/.test(id)) throw new Error();
    const response = await fetch("/api/subscriptions/" + id + "/summary?month=" + month, { cache: "no-store" });
    if (!response.ok) throw new Error();
    const { bill, annualAverage, overallAverage, rules } = await response.json();
    if (typeof bill.name !== "string" || typeof bill.active !== "boolean") throw new Error();
    document.querySelector("#name").textContent = bill.name;
    document.title = bill.name + " · RocketFlyIII";
    const low = amount(bill.amount_min, bill.currency_code);
    const high = amount(bill.amount_max, bill.currency_code);
    document.querySelector("#definition").textContent =
      "Subscription matches transactions " + (low === high ? "of " + low : "between " + low + " and " + high)
      + ". Repeats " + repeats(bill) + ".";
    const table = document.querySelector("#summary");
    for (const [label, value] of [
      ["Subscription is active", bill.active ? "Yes" : "No"],
      ["Next expected match", dateLabel(bill.next_expected_match)],
      ["Average subscription amount (" + month.slice(0, 4) + ")", amount(annualAverage, bill.currency_code)],
      ["Average subscription amount (overall)", amount(overallAverage, bill.currency_code)],
      ["Related rules", rules.join("\n") || "None"]
    ]) {
      const row = table.tBodies[0].insertRow();
      const heading = document.createElement("th");
      heading.scope = "row";
      heading.textContent = label;
      row.append(heading);
      row.insertCell().textContent = value;
    }
    table.hidden = false;
    status.textContent = "";
  } catch {
    status.textContent = "Couldn’t load this subscription. Refresh to try again.";
  }
}
loadSubscription();
