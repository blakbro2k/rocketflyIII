import { api, option, perform } from "./access-ui.js";
const add = document.querySelector("#add");
async function load() {
  const me = await api("/api/me");
  document.querySelector("#current").textContent = me.username + " · " + me.roleName;
  document.querySelector("#management").hidden = me.roleId !== 1;
  if (me.roleId !== 1) return;
  const [roles, users] = await Promise.all([api("/api/admin/roles"), api("/api/admin/profiles")]);
  function choices(select) {
    select.replaceChildren(...roles.map(r => option(r.id, r.name)));
  }
  choices(add.elements.role);
  add.elements.role.prepend(option("", "Choose a role"));
  add.elements.role.value = "";
  const list = document.querySelector("#profiles");
  list.replaceChildren();
  for (const user of users) {
    const row = document.createElement("form");
    const label = document.createElement("label");
    const name = document.createElement("strong");
    name.textContent = user.username;
    label.textContent = "Role";
    const select = document.createElement("select");
    choices(select);
    select.value = user.roleId;
    label.append(select);
    const save = document.createElement("button");
    save.textContent = "Save changes";
    const statusLabel = document.createElement("label");
    statusLabel.textContent = "Status";
    const status = document.createElement("select");
    status.append(option("active", "Active"), option("inactive", "Inactive"));
    status.value = user.active ? "active" : "inactive";
    statusLabel.append(status);
    row.setAttribute("aria-label", "Edit " + user.username);
    row.append(name, label, statusLabel, save);
    row.onsubmit = event => {
      event.preventDefault();
      perform(async () => {
        await api("/api/admin/profiles/" + encodeURIComponent(user.username), "PUT", {roleId: Number(select.value), active: status.value === "active"});
        await load();
        document.querySelector("#feedback").textContent = "Saved changes for " + user.username + ".";
      });
    };
    list.append(row);
  }
}
add.onsubmit = event => {
  event.preventDefault();
  perform(async () => {
    const fields = add.elements;
    if (fields.password.value !== fields.confirm.value) throw new Error("Passwords must match.");
    await api("/api/admin/profiles", "POST", {username: fields.username.value, password: fields.password.value, roleId: Number(fields.role.value)});
    add.reset();
    await load();
    document.querySelector("#feedback").textContent = "Profile created.";
  });
};
perform(load);
