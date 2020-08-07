<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" session="false"
	import="java.io.*,java.util.*, java.text.*,cz.sysnet.pdf.rest.ApplicationFactory"%>
<!DOCTYPE html>
<html lang="cs">
<head>
<meta charset="utf-8">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>PDF Service</title>
<%@include  file="bootstrap.html" %>
</head>
<body>
	<%@include  file="header.jsp" %>
	<div class="container">
		<div class="jumbotron text-center">
			<p class="small">Domovská stránka</p>
			<p class="bg-info small">
				<%
					// Set refresh, autoload time as 10 seconds
					response.setIntHeader("Refresh", 10);

					// Get current time
					Calendar calendar = new GregorianCalendar();
					SimpleDateFormat sdf = new SimpleDateFormat("d.M.yyyy HH:mm:ss");
					String ct = sdf.format(calendar.getTime());
					out.println("Čas poslední aktualizace: " + ct + "\n");
				%>
			</p>
			<p class="bg-primary small"><%=cz.sysnet.pdf.rest.ApplicationFactory.getInstance().getDurationString()%></p>
		</div>
		<div class="panel panel-default">
			<div class="panel-body">
				<!-- <%=cz.sysnet.pdf.rest.ApplicationFactory.getInstance().getHtmlDirTable()%> -->
				<%=cz.sysnet.pdf.rest.ApplicationFactory.getInstance().getHtmlTemplateTable()%>
				<%=cz.sysnet.pdf.rest.ApplicationFactory.getInstance().getHtmlUploadedTable()%>
				<%=cz.sysnet.pdf.rest.ApplicationFactory.getInstance().getHtmlCounterTable()%>
			</div>
			<div class="panel-footer">
				<a class="btn btn-default" role="button" href="api/verify">Verifikace</a>
				<a class="btn btn-info" role="button" href="api/application.wadl">WADL</a>
				<a class="btn btn-primary" role="button" href="api/template">Šablony</a>
				<a class="btn btn-success" role="button" href="index.jsp">Domů</a> 
				<a class="btn btn-danger" role="button" href="upload.jsp">Upload</a> 
			</div>
		</div>
	</div>
</body>
</html>