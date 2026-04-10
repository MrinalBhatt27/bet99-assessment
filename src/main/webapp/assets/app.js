/* global $, API_BASE_URL */

// ── Utilities ────────────────────────────────────────────────────────────────

function apiUrl(path) {
  var base = (window.API_BASE_URL || "").replace(/\/+$/, "");
  if (!path) return base;
  if (path.charAt(0) !== "/") path = "/" + path;
  return base + path;
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function formatDate(iso) {
  try {
    var d = new Date(iso);
    return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" })
      + " " + d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  } catch (e) {
    return iso;
  }
}

// ── Debounce helper ───────────────────────────────────────────────────────────

function debounce(fn, delay) {
  var timer;
  return function () {
    clearTimeout(timer);
    timer = setTimeout(fn, delay);
  };
}

// ── Toast notifications ───────────────────────────────────────────────────────

function showToast(message, isError) {
  var toast = $("<div class='toast" + (isError ? " toast-error" : " toast-success") + "'></div>")
    .text(message);
  $("#toastContainer").append(toast);
  // Trigger reflow so the transition kicks in
  toast[0].getBoundingClientRect();
  toast.addClass("toast-visible");
  setTimeout(function () {
    toast.removeClass("toast-visible");
    setTimeout(function () { toast.remove(); }, 350);
  }, 3000);
}

// ── Inline form message (validation only) ────────────────────────────────────

function setMessage(text, isError) {
  var el = $("#formMessage");
  el.text(text || "");
  el.toggleClass("error", !!isError);
}

// ── Edit-mode state ───────────────────────────────────────────────────────────

var editingId = null;

function setEditMode(bug) {
  editingId = bug.id;
  $("#formTitle").text("Edit Bug #" + bug.id);
  $("#bugTitle").val(bug.bugTitle);
  $("#description").val(bug.description);
  $("#severity").val(bug.severity);
  $("#status").val(bug.status);
  $("#submitBug").text("Update");
  $("#cancelEdit").show();
  setMessage("");
  updateTitleCount();
  $("html, body").animate({ scrollTop: 0 }, 250);
}

function clearEditMode() {
  editingId = null;
  $("#formTitle").text("Log a bug");
  $("#bugTitle").val("");
  $("#description").val("");
  $("#severity").val("MEDIUM");
  $("#status").val("OPEN");
  $("#submitBug").text("Submit");
  $("#cancelEdit").hide();
  setMessage("");
  updateTitleCount();
}

// ── Character counter for title ───────────────────────────────────────────────

var TITLE_MAX = 255;

function updateTitleCount() {
  var len = $("#bugTitle").val().length;
  var el  = $("#titleCount");
  el.text(len + " / " + TITLE_MAX);
  el.toggleClass("count-warn", len > TITLE_MAX * 0.9);
}

// ── Bug cache (keyed by id) ───────────────────────────────────────────────────

var bugsCache = {};

// ── All bugs as returned by the last API call ─────────────────────────────────

var allBugs = [];

// ── Pagination state ──────────────────────────────────────────────────────────

var currentPage = 1;
var pageSize    = 10;

// ── Sort state ────────────────────────────────────────────────────────────────

var sortCol = "id";
var sortDir = "desc";

var SEVERITY_ORDER = { LOW: 0, MEDIUM: 1, HIGH: 2, CRITICAL: 3 };

function applySort(arr) {
  var col = sortCol;
  var dir = sortDir === "asc" ? 1 : -1;
  return arr.slice().sort(function (a, b) {
    var av = a[col];
    var bv = b[col];
    if (col === "severity") { av = SEVERITY_ORDER[av] || 0; bv = SEVERITY_ORDER[bv] || 0; }
    if (av == null) return dir;
    if (bv == null) return -dir;
    if (typeof av === "string") av = av.toLowerCase();
    if (typeof bv === "string") bv = bv.toLowerCase();
    return av < bv ? -dir : av > bv ? dir : 0;
  });
}

function updateSortHeaders() {
  $("#bugsTable thead th[data-col]").each(function () {
    var col = $(this).data("col");
    $(this).find(".sort-icon").remove();
    if (col === sortCol) {
      $(this).append("<span class='sort-icon'>" + (sortDir === "asc" ? " ▲" : " ▼") + "</span>");
    }
  });
}

// ── Table rendering ───────────────────────────────────────────────────────────

var STATUS_OPTIONS = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

function buildStatusSelect(id, currentStatus) {
  var html = "<select class='statusSelect' data-id='" + escapeHtml(String(id)) + "'>";
  for (var i = 0; i < STATUS_OPTIONS.length; i++) {
    var s = STATUS_OPTIONS[i];
    html += "<option value='" + s + "'" + (s === currentStatus ? " selected" : "") + ">" + s + "</option>";
  }
  html += "</select>";
  return html;
}

function renderTable(items) {
  bugsCache = {};
  var tbody = $("#bugsTable tbody");
  tbody.empty();

  if (!items || !items.length) {
    var q = $.trim($("#searchBox").val());
    var msg = q
      ? "No bugs match <em>" + escapeHtml(q) + "</em>"
      : allBugs.length === 0
        ? "<span class='empty-icon'>&#x1F41B;</span><br>No bugs logged yet — submit one above!"
        : "No bugs match the current filters";
    tbody.append("<tr><td colspan='8' class='emptyState'>" + msg + "</td></tr>");
    return;
  }

  for (var i = 0; i < items.length; i++) {
    var b = items[i];
    bugsCache[b.id] = b;
    var row = "<tr>"
      + "<td class='col-meta muted'>" + escapeHtml(b.id) + "</td>"
      + "<td>" + escapeHtml(b.bugTitle) + "</td>"
      + "<td class='col-meta'><span class='pill sev-" + escapeHtml(String(b.severity).toLowerCase()) + "'>"
      + escapeHtml(b.severity) + "</span></td>"
      + "<td class='col-meta'>" + buildStatusSelect(b.id, b.status) + "</td>"
      + "<td class='col-meta muted'>" + escapeHtml(formatDate(b.createdAt)) + "</td>"
      + "<td class='col-desc'>" + escapeHtml(b.description) + "</td>"
      + "<td class='col-action'><button class='btnEdit' data-id='" + escapeHtml(String(b.id)) + "' title='Edit'>&#9998;</button></td>"
      + "<td class='col-action'><button class='btnDelete' data-id='" + escapeHtml(String(b.id)) + "' title='Delete'>&#x1F5D1;</button></td>"
      + "</tr>";
    tbody.append(row);
  }
}

// ── Pagination controls ───────────────────────────────────────────────────────

function renderPagination(total) {
  var pageCount = Math.max(1, Math.ceil(total / pageSize));
  if (currentPage > pageCount) currentPage = pageCount;

  var start = total === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  var end   = Math.min(currentPage * pageSize, total);

  $("#pageInfo").text(start + "–" + end + " of " + total);
  $("#btnPrev").prop("disabled", currentPage <= 1);
  $("#btnNext").prop("disabled", currentPage >= pageCount);
  $("#pagination").toggle(total > 0);
}

// ── Filter + sort + paginate pipeline ────────────────────────────────────────

function filterAndRender() {
  var q = $.trim($("#searchBox").val()).toLowerCase();
  var filtered = allBugs;

  if (q) {
    filtered = [];
    for (var i = 0; i < allBugs.length; i++) {
      var b = allBugs[i];
      if (b.bugTitle.toLowerCase().indexOf(q) !== -1 ||
          b.description.toLowerCase().indexOf(q) !== -1) {
        filtered.push(b);
      }
    }
  }

  filtered = applySort(filtered);

  var total     = filtered.length;
  var pageCount = Math.max(1, Math.ceil(total / pageSize));
  if (currentPage > pageCount) currentPage = pageCount;

  var start = (currentPage - 1) * pageSize;
  renderTable(filtered.slice(start, start + pageSize));
  renderPagination(total);
  updateSortHeaders();
}

// ── API calls ─────────────────────────────────────────────────────────────────

function loadBugs() {
  var sev = $("#severityFilter").val();
  var st  = $("#statusFilter").val();
  var url = apiUrl("/api/v1/bugs");
  var params = [];
  if (sev) params.push("severity=" + encodeURIComponent(sev));
  if (st)  params.push("status="   + encodeURIComponent(st));
  if (params.length) url += "?" + params.join("&");

  $.getJSON(url)
    .done(function (data) { allBugs = data || []; filterAndRender(); })
    .fail(function (xhr) { showToast("Failed to load bugs (" + xhr.status + ")", true); });
}

function updateBugStatus(id, newStatus) {
  $.ajax({
    method: "PATCH",
    url: apiUrl("/api/v1/bugs/" + id + "/status"),
    contentType: "application/json",
    data: JSON.stringify({ status: newStatus })
  })
  .done(function () { showToast("Status updated.", false); })
  .fail(function (xhr) {
    var msg = "Update failed (" + xhr.status + ")";
    if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
    showToast(msg, true);
    loadBugs();
  });
}

function deleteBug(id) {
  var bug   = bugsCache[id];
  var label = bug ? "\u201c" + bug.bugTitle + "\u201d" : "#" + id;
  if (!window.confirm("Delete " + label + "?\nThis cannot be undone.")) return;

  $.ajax({ method: "DELETE", url: apiUrl("/api/v1/bugs/" + id) })
    .done(function () {
      if (editingId === id) clearEditMode();
      showToast("Bug " + label + " deleted.", false);
      loadBugs();
    })
    .fail(function (xhr) {
      var msg = "Delete failed (" + xhr.status + ")";
      if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
      showToast(msg, true);
    });
}

function submitBug() {
  setMessage("");

  var title       = $.trim($("#bugTitle").val());
  var description = $.trim($("#description").val());

  if (!title) {
    setMessage("Title is required.", true);
    $("#bugTitle").focus();
    return;
  }
  if (!description) {
    setMessage("Description is required.", true);
    $("#description").focus();
    return;
  }

  var payload = {
    bugTitle: title,
    description: description,
    severity: $("#severity").val(),
    status: $("#status").val()
  };

  if (editingId !== null) {
    $.ajax({
      method: "PUT",
      url: apiUrl("/api/v1/bugs/" + editingId),
      contentType: "application/json",
      data: JSON.stringify(payload)
    })
    .done(function () {
      clearEditMode();
      showToast("Bug updated successfully.", false);
      loadBugs();
    })
    .fail(function (xhr) {
      var msg = "Update failed (" + xhr.status + ")";
      if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
      showToast(msg, true);
    });

  } else {
    $.ajax({
      method: "POST",
      url: apiUrl("/api/v1/bugs"),
      contentType: "application/json",
      data: JSON.stringify(payload)
    })
    .done(function () {
      clearEditMode();
      showToast("Bug submitted successfully.", false);
      loadBugs();
    })
    .fail(function (xhr) {
      var msg = "Submit failed (" + xhr.status + ")";
      if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
      showToast(msg, true);
    });
  }
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────

$(function () {
  updateTitleCount();

  $("#submitBug").on("click", submitBug);
  $("#cancelEdit").on("click", clearEditMode);

  $("#bugTitle").on("input", updateTitleCount);

  $("#severityFilter").on("change", function () { currentPage = 1; loadBugs(); });
  $("#statusFilter").on("change",   function () { currentPage = 1; loadBugs(); });
  $("#pageSize").on("change", function () {
    pageSize = parseInt($(this).val(), 10);
    currentPage = 1;
    filterAndRender();
  });
  $("#searchBox").on("input", debounce(function () { currentPage = 1; filterAndRender(); }, 250));

  $("#btnPrev").on("click", function () { if (currentPage > 1) { currentPage--; filterAndRender(); } });
  $("#btnNext").on("click", function () { currentPage++; filterAndRender(); });

  // Column sort
  $(document).on("click", "#bugsTable thead th[data-col]", function () {
    var col = $(this).data("col");
    if (sortCol === col) {
      sortDir = sortDir === "asc" ? "desc" : "asc";
    } else {
      sortCol = col;
      sortDir = "asc";
    }
    currentPage = 1;
    filterAndRender();
  });

  // Inline status change (quick PATCH)
  $(document).on("change", ".statusSelect", function () {
    updateBugStatus($(this).data("id"), $(this).val());
  });

  // Edit button → pre-fill the form
  $(document).on("click", ".btnEdit", function () {
    var bug = bugsCache[$(this).data("id")];
    if (bug) setEditMode(bug);
  });

  // Delete button
  $(document).on("click", ".btnDelete", function () {
    deleteBug($(this).data("id"));
  });

  loadBugs();
});
