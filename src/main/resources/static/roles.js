import { api, option, perform } from "./access-ui.js";
const picker = document.querySelector("#roles");
const editor = document.querySelector("#editor");
const name = document.querySelector("#name");
const remove = document.querySelector("#delete");
let roles = [], resources = [];
function draw(selected) {
  const role = roles.find(r => String(r.id) === picker.value);
  name.value = role?.name || "";
  editor.hidden = role?.id === 1;
  remove.hidden = !role;
  const grants = selected ?? role?.grants ?? [];
  for (const kind of ["account", "budget"]) {
    const list = document.getElementById(kind);
    list.replaceChildren();
    const known = new Map(resources.filter(r => r.kind === kind).map(r => [r.id, r.name]));
    for (const g of grants.filter(g => g.kind === kind)) {
      if (!known.has(g.id)) known.set(g.id, "Unavailable " + kind + " #" + g.id);
    }
    for (const [id, title] of known) {
      const label = document.createElement("label"), input = document.createElement("input");
      input.type = "checkbox";
      input.dataset.kind = kind;
      input.value = id;
      input.checked = grants.some(g => g.kind === kind && g.id === id);
      label.append(input, " " + title);
      list.append(label);
    }
  }
}
function checked() {
  return [...editor.querySelectorAll("input:checked")].map(i => ({kind:i.dataset.kind, id:i.value}));
}
async function load() {
  editor.hidden = true;
  [roles, resources] = await Promise.all([api("/api/admin/roles"), api("/api/admin/resources")]);
  picker.replaceChildren(option("", "Add new role"), ...roles.map(r => option(r.id, r.name)));
  draw();
}
picker.onchange = () => draw();
document.querySelector("#refresh").onclick = () => perform(async () => {
  const savedName = name.value, grants = checked();
  resources = await api("/api/admin/resources");
  draw(grants);
  name.value = savedName;
});
editor.onsubmit = event => {
  event.preventDefault();
  perform(async () => {
    const path = "/api/admin/roles" + (picker.value ? "/" + picker.value : "");
    await api(path, picker.value ? "PUT" : "POST", {name:name.value, grants:checked()});
    await load();
    document.querySelector("#feedback").textContent = "Role saved.";
  });
};
remove.onclick = () => perform(async () => {
  if (!picker.value || !confirm("Delete this role?")) return;
  await api("/api/admin/roles/" + picker.value, "DELETE");
  await load();
});
perform(load);
