const refresh = document.querySelector("#refresh");
const statusLine = document.querySelector("#status");
const groups = document.querySelector("#groups");
const tableTemplate = document.querySelector("#account-table");
const roleLabels = new Map([
  ["defaultAsset", "Default asset accounts"],
  ["sharedAsset", "Shared accounts"],
  ["savingAsset", "Savings"],
  ["ccAsset", "Credit cards"],
  ["cashWalletAsset", "Cash wallets"]
]);

async function fetchAccounts(type) {
  const response = await fetch("/api/accounts?type=" + type, { cache: "no-store" });
  if (!response.ok) throw new Error("Account request failed");
  const accounts = await response.json();
  if (!Array.isArray(accounts)) throw new Error("Invalid account list");
  return accounts;
}

function accountGroup(key, label, accounts, collapsed) {
  const section = document.createElement("details");
  section.dataset.group = key;
  section.open = !collapsed.has(key);
  const heading = document.createElement("summary");
  heading.textContent = label + " (" + accounts.length + ")";
  section.append(heading);
  if (!accounts.length) {
    const empty = document.createElement("p");
    empty.textContent = "No liabilities in Firefly III.";
    section.append(empty);
    return section;
  }

  const table = tableTemplate.content.querySelector("table").cloneNode(true);
  table.setAttribute("aria-label", label);
  for (const { attributes } of accounts) {
    const row = table.tBodies[0].insertRow();
    row.insertCell().textContent = attributes.name;
    const amount = attributes.current_balance;
    row.insertCell().textContent = amount == null || amount === ""
      ? "—" : [amount, attributes.currency_code].filter(value => value != null && value !== "").join(" ");
  }
  section.append(table);
  return section;
}

async function loadAccounts() {
  refresh.disabled = true;
  statusLine.textContent = "Loading accounts from Firefly III…";
  try {
    const [assets, liabilities] = await Promise.all([fetchAccounts("asset"), fetchAccounts("liability")]);
    const byRole = new Map();
    for (const account of assets) {
      const role = account.attributes.account_role || "";
      if (!byRole.has(role)) byRole.set(role, []);
      byRole.get(role).push(account);
    }

    const collapsed = new Set(Array.from(groups.querySelectorAll("details:not([open])"), item => item.dataset.group));
    const sections = document.createDocumentFragment();
    const roles = new Set([...roleLabels.keys(), ...byRole.keys()]);
    for (const role of roles) {
      if (byRole.has(role)) {
        sections.append(accountGroup("asset:" + role, roleLabels.get(role) || role || "Unassigned role", byRole.get(role), collapsed));
      }
    }
    sections.append(accountGroup("liability", "Liabilities", liabilities, collapsed));
    groups.replaceChildren(sections);
    const count = assets.length + liabilities.length;
    statusLine.textContent = count + (count === 1 ? " account" : " accounts") + " from Firefly III";
  } catch {
    statusLine.textContent = groups.childElementCount
      ? "Refresh failed. Showing the previous balances."
      : "Couldn’t load accounts from Firefly III. Select Refresh to try again.";
  } finally {
    refresh.disabled = false;
  }
}

refresh.addEventListener("click", loadAccounts);
loadAccounts();
