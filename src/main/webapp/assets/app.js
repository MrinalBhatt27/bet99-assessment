/* global $, API_BASE_URL */

// ── Utilities ────────────────────────────────────────────────────────────────

function apiUrl(path) {
  var base = (window.API_BASE_URL || "").replace(/\/+$/, "");
  if (!path) return base;
  if (path.charAt(0) !== "/") path = "/" + path;
  return base + path;
}

function setMessage(text, isError) {
  var el = $("#formMessage");
  el.text(text || "");
  el.toggleClass("error", !!isError);
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

// ── Edit-mode state ───────────────────────────────────────────────────────────

var editingId = null; // null = create mode, number = edit mode

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
}

// ── Bug cache (keyed by id, populated after each loadBugs) ───────────────────

var bugsCache = {};

// ── All bugs as returned by the last API call (search filters this) ───────────

var allBugs = [];

// ── Pagination state ──────────────────────────────────────────────────────────

var currentPage = 1;
var pageSize    = 10;

// ── Debounce helper ───────────────────────────────────────────────────────────

function debounce(fn, delay) {
  var timer;
  return function () {
    clearTimeout(timer);
    timer = setTimeout(fn, delay);
  };
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
    tbody.append("<tr><td colspan='8' class='muted'>No bugs yet</td></tr>");
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

// ── API calls ─────────────────────────────────────────────────────────────────

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

  var total     = filtered.length;
  var pageCount = Math.max(1, Math.ceil(total / pageSize));
  if (currentPage > pageCount) currentPage = pageCount;

  var start = (currentPage - 1) * pageSize;
  renderTable(filtered.slice(start, start + pageSize));
  renderPagination(total);
}

function loadBugs() {
  var sev = $("#severityFilter").val();
  var st  = $("#statusFilter").val();
  var url = apiUrl("/api/bugs");
  var params = [];
  if (sev) params.push("severity=" + encodeURIComponent(sev));
  if (st)  params.push("status="   + encodeURIComponent(st));
  if (params.length) url += "?" + params.join("&");

  $.getJSON(url)
    .done(function (data) { allBugs = data || []; filterAndRender(); })
    .fail(function (xhr) { setMessage("Failed to load bugs (" + xhr.status + ")", true); });
}

function updateBugStatus(id, newStatus) {
  $.ajax({
    method: "PATCH",
    url: apiUrl("/api/bugs/" + id + "/status"),
    contentType: "application/json",
    data: JSON.stringify({ status: newStatus })
  })
  .done(function () { setMessage("Status updated.", false); })
  .fail(function (xhr) {
    var msg = "Update failed (" + xhr.status + ")";
    if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
    setMessage(msg, true);
    loadBugs();
  });
}

function deleteBug(id) {
  if (!window.confirm("Delete bug #" + id + "?")) return;

  $.ajax({ method: "DELETE", url: apiUrl("/api/bugs/" + id) })
    .done(function () {
      if (editingId === id) clearEditMode();
      setMessage("Bug #" + id + " deleted.", false);
      loadBugs();
    })
    .fail(function (xhr) {
      var msg = "Delete failed (" + xhr.status + ")";
      if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
      setMessage(msg, true);
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
    // ── Update existing bug (PUT) ────────────────────────────────────────────
    $.ajax({
      method: "PUT",
      url: apiUrl("/api/bugs/" + editingId),
      contentType: "application/json",
      data: JSON.stringify(payload)
    })
    .done(function () {
      clearEditMode();
      setMessage("Bug updated successfully.", false);
      loadBugs();
    })
    .fail(function (xhr) {
      var msg = "Update failed (" + xhr.status + ")";
      if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
      setMessage(msg, true);
    });

  } else {
    // ── Create new bug (POST) ────────────────────────────────────────────────
    $.ajax({
      method: "POST",
      url: apiUrl("/api/bugs"),
      contentType: "application/json",
      data: JSON.stringify(payload)
    })
    .done(function () {
      $("#bugTitle").val("");
      $("#description").val("");
      $("#severity").val("MEDIUM");
      $("#status").val("OPEN");
      setMessage("Bug submitted successfully.", false);
      loadBugs();
    })
    .fail(function (xhr) {
      var msg = "Submit failed (" + xhr.status + ")";
      if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
      setMessage(msg, true);
    });
  }
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────

$(function () {
  $("#submitBug").on("click", submitBug);

  $("#cancelEdit").on("click", clearEditMode);

  $("#severityFilter").on("change", function () { setMessage(""); currentPage = 1; loadBugs(); });
  $("#statusFilter").on("change",   function () { setMessage(""); currentPage = 1; loadBugs(); });
  $("#pageSize").on("change", function () {
    pageSize = parseInt($(this).val(), 10);
    currentPage = 1;
    filterAndRender();
  });
  $("#searchBox").on("input", debounce(function () { currentPage = 1; filterAndRender(); }, 250));

  $("#btnPrev").on("click", function () { if (currentPage > 1) { currentPage--; filterAndRender(); } });
  $("#btnNext").on("click", function () { currentPage++; filterAndRender(); });

  // Inline status change (quick PATCH)
  $(document).on("change", ".statusSelect", function () {
    updateBugStatus($(this).data("id"), $(this).val());
  });

  // Edit button → pre-fill the form at the top
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
