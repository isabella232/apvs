<%@page session="true"%>
<!doctype html>
<%
	long current = new java.util.Date().getTime();
	session.setAttribute("time", new Long(current));
%>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<!--                                           -->
<!-- Any title is fine                         -->
<!--                                           -->
<title>APVS [<%=current%>] SessionID[<%=session.getId()%>]
</title>

<link rel="stylesheet" type="text/css" href="css/APVS.css" />

<!--                                           -->
<!-- This script loads your compiled module.   -->
<!-- If you add any GWT meta tags, they must   -->
<!-- be added before this line.                -->
<!--                                           -->
<script type="text/javascript"
	src="apvs.nocache.js"></script>
</head>

<!--                                           -->
<!-- The body can have arbitrary html, or      -->
<!-- you can leave the body empty if you want  -->
<!-- to create a completely dynamic UI.        -->
<!--                                           -->
<body>

	<!-- OPTIONAL: include this if you want history support -->
	<iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1'
		style="position: absolute; width: 0; height: 0; border: 0"></iframe>

	<noscript>
		<div
			style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
			Your web browser must have JavaScript enabled in order for APVS to display correctly.</div>
	</noscript>

</body>
</html>
