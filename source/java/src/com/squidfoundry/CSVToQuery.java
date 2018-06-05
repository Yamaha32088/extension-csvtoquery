package com.squidfoundry;

import lucee.commons.io.IOUtil;
import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.ext.function.Function;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.QueryImpl;
import lucee.runtime.util.Cast;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CSVToQuery extends BIF implements Function {

	private static final long serialVersionUID = -7445855447141608696L;
	
	private CFMLEngine engine;
	private Cast caster;
	
	private static boolean csvIsString;
	private static boolean csvIsFile;

	public static Object call(PageContext pc, String csv) throws PageException, IOException {
		return _call(pc, csv, "", true, true, null);
	}
	
	public static Object call(PageContext pc, String csv, String filePath) throws PageException, IOException {
		return _call(pc, csv, filePath, true, true, null);
	}

	public static Object call(PageContext pc, String csv, String filePath, Boolean firstRowIsHeader) throws PageException, IOException {
		return _call(pc, csv, filePath, firstRowIsHeader, true, null);
	}
	
	public static Object call(PageContext pc, String csv, String filePath, Boolean firstRowIsHeader, Boolean trim) throws PageException, IOException {
		return _call(pc, csv, filePath, firstRowIsHeader, trim, null);
	}
	
	public static Object call(PageContext pc, String csv, String filePath, Boolean firstRowIsHeader, Boolean trim, Character delimiter) throws PageException, IOException {
		return _call(pc, csv, filePath, firstRowIsHeader, trim, delimiter);
	}
	
	public static Object _call(PageContext pc, String csv, String filePath, Boolean firstRowIsHeader, Boolean trim, Character delimiter) throws PageException, IOException {
		String[] headers = null;
		
		checkInput(csv, filePath);
		
		if(csvIsFile)
			csv = getCSVFromFile(pc, filePath);
		
		if(trim)
			csv = csv.trim();
		
		List<CSVRecord> records = parseCSV(csv, delimiter);
		
		int numRows = records.size();
		
		if(numRows == 0 && firstRowIsHeader) {
			throw emptyCSV();
		}

		CSVRecord row = records.get(0);
		int numCols = row.size();
		int curRow = 0;
		
		if(firstRowIsHeader) {
			curRow++;
			if(headers == null) {
				headers = makeUnique( row );
			}
		}

		if(headers == null) {
			headers = new String[numCols];
			for(int i = 0; i < numCols; i++) {
				headers[i] = "COLUMN_" + (i + 1);
			}
		}
		
		Array[] rows = new Array[numCols];
		for(int i = 0; i < numCols; i++) {
			rows[i] = new ArrayImpl();
		}
		
		while(curRow < numRows) {
			row = records.get(curRow++);
			if(row.size() != numCols) {
				String exceptionMessage = "Invalid number of columns, expected " + numCols + " columns but found " + row.size();
				throw customException(exceptionMessage, "Number of columns did not match", "CSVToQuery");
			}
			for(int i = 0; i < numCols; i++) {
				rows[i].append(row.get(i));
			}
		}
		
		return new QueryImpl(headers, rows, "query");
	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {
		engine = CFMLEngineFactory.getInstance();
		caster=engine.getCastUtil();
		try {
			if(args.length==1)
				return call(pc, caster.toString(args[0]));
			if(args.length==2) 
				return call(pc, caster.toString(args[0]), caster.toString(args[1]));
			if(args.length==3)
				return call(pc, caster.toString(args[0]), caster.toString(args[1]), caster.toBoolean(args[2]));
			if(args.length==4)
				return call(pc, caster.toString(args[0]), caster.toString(args[1]), caster.toBoolean(args[2]), caster.toBoolean(args[3]));
			if(args.length==5)
				return call(pc, caster.toString(args[0]), caster.toString(args[1]), caster.toBoolean(args[2]), caster.toBoolean(args[3]), caster.toCharacter(args[4]));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		throw engine.getExceptionUtil().createFunctionException(pc, "CSVToQuery", 1, 5, args.length);
	}
	
	private static List<CSVRecord> parseCSV(String csv, Character delimiter) throws IOException {
		CSVParser parsed = CSVParser.parse(csv, getCSVFormat(delimiter));
		return parsed.getRecords();
	}
	
	private static CSVFormat getCSVFormat(Character delimiter) {
		CSVFormat format = CSVFormat.RFC4180;
		format = format.withIgnoreSurroundingSpaces();
		format = (delimiter != null) ? format.withDelimiter(delimiter) : format;
		return format;
	}
	
	private static String getCSVFromFile(PageContext pc, String filePath) throws PageException {
		Resource res = Caster.toResource(pc, filePath, true);
		pc.getConfig().getSecurityManager().checkFileLocation(res);
		try {
			return IOUtil.toString(res, pc.getResourceCharset());
		} catch(IOException e) {
			throw Caster.toPageException(e);
		}
	}
	
	private static void checkInput(String csv, String filePath) throws PageException {
		csvIsString = !csv.isEmpty();
		csvIsFile = !filePath.isEmpty();
		
		if(!csvIsString && !csvIsFile) {
			throw missingRequiredArguments();
		}
		
		if(csvIsString && csvIsFile) {
			throw mutuallyExclusive();
		}
	}
	
	private static PageException customException(String message, String detail, String type) {
		return CFMLEngineFactory.getInstance().getExceptionUtil().createCustomTypeException(message, detail, null, type, null);
	}
	
	private static PageException emptyCSV() {
		return customException("CSV did not contain any rows", "The CSV provided was empty", "CSVToQuery");
	}
	
	private static PageException missingRequiredArguments() {
		return customException(
				"Missing required argument", 
				"Please provide either a csv string (csv), or the path of a file containing one (filepath).", 
				"CSVToQuery"
		);
	}
	
	private static PageException mutuallyExclusive() {
		return customException(
				"Mutually exclusive arguments: 'csv' and 'filepath'", 
				"Only one of either 'filepath' or 'csv' arguments may be provided.",
				"CSVToQuery"
		);
	}
	
	private static String[] makeUnique(CSVRecord headers) {
		String[] headerList = new String[headers.size()];
		for(int i = 0; i < headers.size(); i++) {
			headerList[i] = headers.get(i);
		}
		
		int c = 1;
		Set set = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		String header, orig;
		
		for(int i=0; i<headerList.length; i++) {
			orig = header = headerList[i];
			
			while(set.contains(header)) {
				header = orig + "_" + ++c;
			}
			
			set.add(header);
			
			if(header != orig) {
				headerList[i] = header;
			}
			
		}
		
		return headerList;
	}

}
