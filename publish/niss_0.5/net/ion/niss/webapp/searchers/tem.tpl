
<ul>
	<li>elapsedTime : ${response.elapsedTime()}
	<li>size : ${response.size()}
	<li>totalCount : ${response.totalCount()}
	<li>params : ${params}
</ul>

<table>
<tr>
	<th>Id</th>
	<th>Title</th>
</tr>
${foreach response.getDocument() doc ,}
<tr>
	<td>${doc.getField(id).toString()}</td>
	<td>${doc.getField(title).toString()}</td>
</tr>
${end}
</table>