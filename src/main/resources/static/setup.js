import { api, perform } from "./access-ui.js";
document.querySelector("#setup").onsubmit = event => {
  event.preventDefault();
  perform(async () => {
    const data = Object.fromEntries(new FormData(event.target));
    if (data.password !== data.confirm) throw new Error("Passwords must match.");
    await api("/api/setup", "POST", { username: data.username, password: data.password, token: data.token });
    location.replace("/login");
  });
};
