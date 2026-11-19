const refresh = document.querySelector("#refresh");
const statusLine = document.querySelector("#status");
const table = document.querySelector("table");
const rows = table.tBodies[0];

async function loadAccounts() {
  refresh.disabled = true;
  table.hidden = true;
  rows.replaceChildren();
  statusLine.textContent = "Loading accounts from Firefly III…";
  try {
    const response = await fetch("/api/accounts", { cache: "no-store" });
    if (!response.ok) throw new Error("Account request failed");
    const accounts = await response.json();
    if (!Array.isArray(accounts)) throw new Error("Invalid account list");

    for (const { attributes } of accounts) {
      const row = rows.insertRow();
      row.insertCell().textContent = attributes.name;
      const amount = attributes.current_balance;
      const currency = attributes.currency_code;
      row.insertCell().textContent = amount == null || amount === ""
        ? "—"
        : [amount, currency].filter(value => value != null && value !== "").join(" ");
    }
    table.hidden = accounts.length === 0;
    statusLine.textContent = accounts.length
      ? accounts.length + (accounts.length === 1 ? " account" : " accounts") + " from Firefly III"
      : "Firefly III returned no asset accounts.";
  } catch {
    statusLine.textContent = "Couldn’t load accounts from Firefly III. Select Refresh to try again.";
  } finally {
    refresh.disabled = false;
  }
}

refresh.addEventListener("click", loadAccounts);
loadAccounts();
