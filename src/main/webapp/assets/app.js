/* global $, API_BASE_URL */

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
  var tbody = $("#bugsTable tbody");
  tbody.empty();

  if (!items || !items.length) {
    tbody.append("<tr><td colspan='7' class='muted'>No bugs yet</td></tr>");
    return;
  }

  for (var i = 0; i < items.length; i++) {
    var b = items[i];
    var row = "<tr>"
      + "<td class='col-meta muted'>" + escapeHtml(b.id) + "</td>"
      + "<td>" + escapeHtml(b.bugTitle) + "</td>"
      + "<td class='col-meta'><span class='pill sev-" + escapeHtml(String(b.severity).toLowerCase()) + "'>"
      + escapeHtml(b.severity) + "</span></td>"
      + "<td class='col-meta'>" + buildStatusSelect(b.id, b.status) + "</td>"
      + "<td class='col-meta muted'>" + escapeHtml(formatDate(b.createdAt)) + "</td>"
      + "<td class='col-desc'>" + escapeHtml(b.description) + "</td>"
      + "<td class='col-action'><button class='btnDelete' data-id='" + escapeHtml(String(b.id)) + "' title='Delete'>&#x1F5D1;</button></td>"
      + "</tr>";
    tbody.append(row);
  }
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
    .done(function (data) {
      renderTable(data);
    })
    .fail(function (xhr) {
      setMessage("Failed to load bugs (" + xhr.status + ")", true);
    });
}

function updateBugStatus(id, newStatus) {
  $.ajax({
    method: "PATCH",
    url: apiUrl("/api/bugs/" + id + "/status"),
    contentType: "application/json",
    data: JSON.stringify({ status: newStatus })
  })
  .done(function () {
    setMessage("Status updated.", false);
  })
  .fail(function (xhr) {
    var msg = "Update failed (" + xhr.status + ")";
    if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
    setMessage(msg, true);
    loadBugs();
  });
}

function deleteBug(id) {
  if (!window.confirm("Delete bug #" + id + "?")) return;

  $.ajax({
    method: "DELETE",
    url: apiUrl("/api/bugs/" + id)
  })
  .done(function () {
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

  var title = $.trim($("#bugTitle").val());
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

$(function () {
  $("#submitBug").on("click", submitBug);

  $("#severityFilter").on("change", function () {
    setMessage("");
    loadBugs();
  });

  $("#statusFilter").on("change", function () {
    setMessage("");
    loadBugs();
  });

  // Event delegation: status dropdowns and delete buttons are created dynamically per row
  $(document).on("change", ".statusSelect", function () {
    var id = $(this).data("id");
    var newStatus = $(this).val();
    updateBugStatus(id, newStatus);
  });

  $(document).on("click", ".btnDelete", function () {
    var id = $(this).data("id");
    deleteBug(id);
  });

  loadBugs();
});
