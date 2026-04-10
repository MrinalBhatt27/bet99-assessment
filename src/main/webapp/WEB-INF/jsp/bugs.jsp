<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Bug Tracker</title>
  <link rel="stylesheet" href="<c:url value='/assets/style.css'/>">
  <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
</head>
<body>
  <div class="container">
    <h1>Bug Tracker</h1>

    <div class="panel">
      <h2>Log a bug</h2>

      <div class="row">
        <label>Title</label>
        <input id="bugTitle" type="text" maxlength="255" placeholder="Short title">
      </div>

      <div class="row">
        <label>Description</label>
        <textarea id="description" rows="4" placeholder="Describe the bug"></textarea>
      </div>

      <div class="row grid2">
        <div>
          <label>Severity</label>
          <select id="severity">
            <option value="LOW">LOW</option>
            <option value="MEDIUM" selected>MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
        </div>
        <div>
          <label>Status</label>
          <select id="status">
            <option value="OPEN" selected>OPEN</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="RESOLVED">RESOLVED</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </div>
      </div>

      <div class="actions">
        <button id="submitBug" type="button">Submit</button>
        <span id="formMessage" class="message"></span>
      </div>
    </div>

    <div class="panel">
      <div class="panelHeader">
        <h2>All bugs</h2>
        <div class="filters">
          <div class="filter">
            <label>Severity</label>
            <select id="severityFilter">
              <option value="">ALL</option>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div class="filter">
            <label>Status</label>
            <select id="statusFilter">
              <option value="">ALL</option>
              <option value="OPEN">OPEN</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>
        </div>
      </div>

      <div class="tableWrapper">
        <table class="table" id="bugsTable">
          <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Created</th>
            <th>Description</th>
          </tr>
          </thead>
          <tbody></tbody>
        </table>
      </div>
    </div>
  </div>

  <script>
    window.API_BASE_URL = "${apiBaseUrl}";
  </script>
  <script src="<c:url value='/assets/app.js'/>"></script>
</body>
</html>

