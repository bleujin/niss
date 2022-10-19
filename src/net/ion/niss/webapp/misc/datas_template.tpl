<!--craken.tpl-->
<!doctype html>
<html>
<head>
</head>
<body>

	<h4>Parent</h4>
	<a href='/open/datas${self.parent().fqn()}.template'>${self.parent().fqn()}</a><br/>

	<h4>Self</h4>
	<a href='/open/datas${self.fqn()}.template'>${self.fqn()}</a><br/>

	<h4>Properties</h4>
	<ul>
	${foreach self.toMap() entry }
		<li>${entry.getKey().idString()} : ${entry.getValue().asString()}</li>
		${if entry.getValue().isBlob()} <a href='/open/datas${self.fqn()}.${entry.getKey().idString()}'>view blob</a> ${end}
	${end}
	</ul>

    <h4>Children</h4>
	<ul>
	${foreach self.children().stream().skip(0).limit(50) child }
		    <li><a href='/open/datas${child.fqn}.template'>${child.fqn}</a></li>
	${end}</ul>
    
</body>
</html>