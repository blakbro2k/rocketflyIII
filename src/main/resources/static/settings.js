import { currentMonth } from "./budget-chart.js";

const drawer = document.querySelector("#settings");
export const monthPicker = drawer.querySelector("input");
const url = new URL(location.href);
let saved;
try { saved = sessionStorage.getItem("budget-month"); } catch {}
const month = url.searchParams.get("month") || saved;
monthPicker.value = month || currentMonth();
if (!monthPicker.value) monthPicker.value = currentMonth();
document.querySelector("#open-settings").onclick = () => drawer.showModal();
drawer.querySelector("button").onclick = () => drawer.close();
monthPicker.addEventListener("change", () => {
  if (!monthPicker.value) monthPicker.value = currentMonth();
  try { sessionStorage.setItem("budget-month", monthPicker.value); } catch {}
  url.searchParams.set("month", monthPicker.value);
  location.replace(url);
});
