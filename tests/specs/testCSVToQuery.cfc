component displayname="My test suite" extends="testbox.system.BaseSpec" {

	function testNoDuplicateHeaderRows() {
		var duplicateHeaderCSV = FileRead('data/duplicateHeader.csv');
		var results = CSVToQuery(csv=duplicateHeaderCSV, firstRowIsHeader=true);
		$assert.isTrue( results.columnExists('Testing_2') )
	}

	function testFirstRowIsHeader() {
		var headerRowWithData = FileRead('data/headerRowWithData.csv');
		var results = CSVToQuery(csv=headerRowWithData, firstRowIsHeader=true);
		$assert.isTrue( results.columnExists('Testing1') )
		$assert.isTrue( results.columnExists('Testing2') )

		var data = results.rowData(1);
		$assert.isTrue( data.Testing1 == 'value1' )
		$assert.isTrue( data.Testing2 == 'value2' )
	}

	function testFirstRowIsNotHeader() {
		var nonHeaderRowWithData = FileRead('data/nonHeaderRowWithData.csv');
		var results = CSVToQuery(csv=nonHeaderRowWithData);
		$assert.isTrue( results.columnExists('Column_1') )
		$assert.isTrue( results.columnExists('Column_2') )

		var data = results.rowData(1);
		$assert.isTrue( data.Column_1 == 'value1' )
		$assert.isTrue( data.Column_2 == 'value2' )

		var data = results.rowData(2);
		$assert.isTrue( data.Column_1 == 'value3' )
		$assert.isTrue( data.Column_2 == 'value4' )
	}

	function testUsingPipeDelimieter() {
		var pipeDelimitedFile = FileRead('data/pipeDelimited.csv');
		var results = CSVToQuery(csv=pipeDelimitedFile, delimiter="|", firstRowIsHeader=true);
		$assert.isTrue( results.columnExists('Testing1') )
		$assert.isTrue( results.columnExists('Testing2') )

		var data = results.rowData(1);
		$assert.isTrue( data.Testing1 == 'value1' )
		$assert.isTrue( data.Testing2 == 'value2' )
	}

	function testUsingFilePath() {
		var results = CSVToQuery(filepath='data/headerRowWithData.csv', firstRowIsHeader=true);
		$assert.isTrue( results.columnExists('Testing1') )
		$assert.isTrue( results.columnExists('Testing2') )

		var data = results.rowData(1);
		$assert.isTrue( data.Testing1 == 'value1' )
		$assert.isTrue( data.Testing2 == 'value2' )
	}

}