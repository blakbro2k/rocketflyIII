export function amount(value, currency) {
  if (value == null || value === "" || !currency || !Number.isFinite(Number(value))) return "—";
  return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(value);
}

export function dateLabel(value) {
  if (!value) return "—";
  const date = new Date(value.slice(0, 10) + "T12:00:00");
  return date.toLocaleDateString(undefined, { month: "long", day: "numeric", year: "numeric" });
}

export function repeats(bill) {
  const units = { weekly: "weeks", monthly: "months", quarterly: "quarters",
    yearly: "years", "half-year": "half-years", daily: "days" };
  const frequency = bill.repeat_freq;
  return bill.skip > 0 && units[frequency]
    ? "every " + (Number(bill.skip) + 1) + " " + units[frequency] : frequency || "—";
}

export function paymentStatus(bill, month) {
  const payments = bill.paid_dates?.filter(p => p.date?.startsWith(month + "-"));
  if (payments?.length) return "Paid · " + payments.map(p => amount(p.amount, p.currency_code || bill.currency_code)).join(", ");
  const due = bill.pay_dates?.find(date => date.startsWith(month + "-"));
  return due ? "Expected " + dateLabel(due) : "—";
}
