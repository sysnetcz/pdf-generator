<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Upload Result</title>
<%@include  file="bootstrap.html" %>
</head>
<body>
	<%@include  file="header.jsp" %>
	<div class="container">
		<div class="alert alert-${alert}">
			<strong>${alertText}</strong> ${message}
		</div>
		<br />
        <a class="btn btn-success" role="button" href="index.jsp">Domů</a><br />
	</div>
</body>
</html>