(function () {
  const root = document.querySelector("[data-ev-root]");
  const backdrop = document.querySelector("[data-ev-modal-backdrop]");
  if (!root || !backdrop) {
    return;
  }

  const status = backdrop.querySelector("[data-ev-modal-status]");
  const content = backdrop.querySelector("[data-ev-modal-content]");
  const chart = backdrop.querySelector("[data-ev-chart]");
  const table = backdrop.querySelector("[data-ev-breakdown-table]");
  const summary = backdrop.querySelector("[data-ev-metric-summary]");
  const title = document.getElementById("ev-modal-title");
  const asOf = root.getAttribute("data-as-of") || "";

  const money = (value) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      maximumFractionDigits: 0
    }).format(Number(value || 0));

  const variance = (value) => {
    const amount = Number(value || 0);
    const formatted = new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      maximumFractionDigits: 0
    }).format(Math.abs(amount));
    return `${amount >= 0 ? "+" : "-"}${formatted.replace("$", "$")}`;
  };

  const showLoading = (message) => {
    status.hidden = false;
    status.textContent = message;
    content.hidden = true;
  };

  const showError = (message) => {
    showLoading(message);
  };

  const openModal = () => {
    backdrop.hidden = false;
    document.body.style.overflow = "hidden";
  };

  const closeModal = () => {
    backdrop.hidden = true;
    document.body.style.overflow = "";
  };

  const metricChip = (label, value) =>
    `<div class="ev-metric-chip"><span class="ev-metric-chip-label">${label}</span><span class="ev-metric-chip-value">${value}</span></div>`;

  const buildChart = (months, bcws, bcwp) => {
    chart.innerHTML = "";
    const allValues = [...bcws, ...bcwp].map(Number);
    const max = Math.max(...allValues, 1);

    months.forEach((month, index) => {
      const planned = Number(bcws[index] || 0);
      const earned = Number(bcwp[index] || 0);
      const plannedHeight = Math.max((planned / max) * 100, planned > 0 ? 6 : 0);
      const earnedHeight = Math.max((earned / max) * 100, earned > 0 ? 6 : 0);

      const group = document.createElement("div");
      group.className = "ev-chart-group";
      group.innerHTML = `
        <div class="ev-chart-bars">
          <div class="ev-chart-bar planned" style="height:${plannedHeight}%"></div>
          <div class="ev-chart-bar earned" style="height:${earnedHeight}%"></div>
        </div>
        <div class="ev-chart-label">${month}</div>
      `;
      chart.appendChild(group);
    });
  };

  const buildTable = (months, data) => {
    const rows = [
      { label: "BCWS", values: data.bcwsByMonth, format: money, className: "" },
      { label: "BCWP", values: data.bcwpByMonth, format: money, className: "" },
      { label: "ACWP", values: data.acwpByMonth, format: money, className: "" },
      { label: "SV", values: data.svByMonth, format: variance, className: "variance" },
      { label: "CV", values: data.cvByMonth, format: variance, className: "variance" }
    ];

    const headCells = months.map((month) => `<th>${month}</th>`).join("");
    const bodyRows = rows
      .map((row) => {
        const cells = months
          .map((_, index) => {
            const raw = row.values[index] || 0;
            const numeric = Number(raw);
            const klass =
              row.className !== "variance"
                ? ""
                : numeric < 0
                  ? " class=\"ev-var-negative\""
                  : " class=\"ev-var-positive\"";
            return `<td${klass}>${row.format(raw)}</td>`;
          })
          .join("");
        return `<tr><th scope="row">${row.label}</th>${cells}</tr>`;
      })
      .join("");

    table.innerHTML = `<thead><tr><th>Metric</th>${headCells}</tr></thead><tbody>${bodyRows}</tbody>`;
  };

  const renderModal = (data) => {
    const months = Array.isArray(data.months) ? data.months : [];
    const monthLabels = months.map((month, index) => month || `M${index + 1}`);

    title.textContent = `${data.wpName || "Work Package"} Monthly Performance`;
    summary.innerHTML = [
      metricChip("BAC", money(data.bac)),
      metricChip("ETC", money(data.etc)),
      metricChip("EAC", money(data.eac)),
      metricChip("VAC", variance(data.vac))
    ].join("");

    buildChart(monthLabels, data.bcwsByMonth || [], data.bcwpByMonth || []);
    buildTable(monthLabels, data);

    status.hidden = true;
    content.hidden = false;
  };

  const loadModal = async (wpId) => {
    showLoading("Loading work package details...");
    openModal();

    try {
      const url = `${window.location.pathname}?handler=WorkPackageModal&wpId=${encodeURIComponent(wpId)}&asOf=${encodeURIComponent(asOf)}`;
      const response = await fetch(url, {
        headers: {
          "X-Requested-With": "XMLHttpRequest"
        }
      });

      const payload = await response.json().catch(() => null);
      if (!response.ok || !payload) {
        throw new Error(payload && payload.message ? payload.message : "Unable to load work package details.");
      }

      renderModal(payload);
    } catch (error) {
      showError(error.message || "Unable to load work package details.");
    }
  };

  document.addEventListener("click", (event) => {
    const trigger = event.target.closest("[data-ev-modal-open]");
    if (trigger) {
      event.preventDefault();
      loadModal(trigger.getAttribute("data-wp-id") || "");
      return;
    }

    if (
      event.target.closest("[data-ev-modal-close]") ||
      event.target === backdrop
    ) {
      closeModal();
    }
  });

  document.addEventListener("keydown", (event) => {
    const trigger = event.target.closest("[data-ev-modal-open]");
    if (trigger && (event.key === "Enter" || event.key === " ")) {
      event.preventDefault();
      loadModal(trigger.getAttribute("data-wp-id") || "");
      return;
    }

    if (event.key === "Escape" && !backdrop.hidden) {
      closeModal();
    }
  });
})();
