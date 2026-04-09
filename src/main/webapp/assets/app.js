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

function renderTable(items) {
  var tbody = $("#bugsTable tbody");
  tbody.empty();

  if (!items || !items.length) {
    tbody.append("<tr><td colspan='6' class='muted'>No bugs yet</td></tr>");
    return;
  }

  for (var i = 0; i < items.length; i++) {
    var b = items[i];
    var row = "<tr>"
      + "<td class='col-meta muted'>" + escapeHtml(b.id) + "</td>"
      + "<td>" + escapeHtml(b.bugTitle) + "</td>"
      + "<td class='col-meta'><span class='pill sev-" + escapeHtml(String(b.severity).toLowerCase()) + "'>"
      + escapeHtml(b.severity) + "</span></td>"
      + "<td class='col-meta'>" + escapeHtml(b.status) + "</td>"
      + "<td class='col-meta muted'>" + escapeHtml(formatDate(b.createdAt)) + "</td>"
      + "<td class='col-desc'>" + escapeHtml(b.description) + "</td>"
      + "</tr>";
    tbody.append(row);
  }
}

function loadBugs() {
  var sev = $("#severityFilter").val();
  var url = apiUrl("/api/bugs");
  if (sev) url += "?severity=" + encodeURIComponent(sev);

  $.getJSON(url)
    .done(function (data) {
      renderTable(data);
    })
    .fail(function (xhr) {
      setMessage("Failed to load bugs (" + xhr.status + ")", true);
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
  loadBugs();
});

