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
    label.textContent = user.username;
    const select = document.createElement("select");
    choices(select);
    select.value = user.roleId;
    label.append(select);
    const save = document.createElement("button");
    save.textContent = "Assign role";
    row.append(label, save);
    row.onsubmit = event => {
      event.preventDefault();
      perform(async () => {
        await api("/api/admin/profiles/" + encodeURIComponent(user.username) + "/role", "PUT", {roleId: Number(select.value)});
        await load();
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
  });
};
perform(load);
