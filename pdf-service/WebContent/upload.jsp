<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Template files upload</title>
<%@include  file="bootstrap.html" %>
</head>
<body>
	<%@include  file="header.jsp" %>
	<div class="container">
		<form method="post" action="uploadFile" enctype="multipart/form-data">
		    <div class="form-group">
		    	<label for="key">Klíč</label>
		    	<input type="text" class="form-control" id="key" name="key">
		    </div>
		    <div class="form-group">
		    	<label for="description">Popis</label>
		    	<input type="text" class="form-control" id="description" name="description">
		    </div>
			<div class="form-group">
				<label for="uploadFile">Vyberte soubor</label>
				<input class="form-control" type="file" name="uploadFile" id="uploadFile" placeholder="vyberte soubor pro nahrání" /> 
			</div>
			<input class="btn btn-danger" role="button" type="submit" value="Upload" />
			<a class="btn btn-success" role="button" href="index.jsp">Domů</a><br />		
		</form>
	</div>
</body>
</html>