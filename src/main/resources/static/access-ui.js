export async function api(path, method = "GET", body) {
  const options = { method, cache: "no-store", headers: {} };
  if (method !== "GET") {
    const csrf = await api("/api/csrf");
    options.headers[csrf.header] = csrf.token;
    if (body) {
      options.headers["Content-Type"] = "application/json";
      options.body = JSON.stringify(body);
    }
  }
  const response = await fetch(path, options);
  if (response.redirected && new URL(response.url).pathname === "/login") {
    location.replace("/login");
    throw new Error("Please sign in.");
  }
  if (!response.ok) {
    const messages = {400: "Check the values and try again.", 403: "Access denied.", 409: "This name is already used, or the role is assigned to a profile."};
    throw new Error(messages[response.status] || "The request failed. Try again.");
  }
  return response.headers.get("content-type")?.includes("application/json") ? response.json() : null;
}
export function option(value, label) { return new Option(label, value); }

export async function perform(action) {
  const status = document.querySelector("#feedback");
  status.textContent = "";
  try { await action(); } catch (error) { status.textContent = error.message; }
}
