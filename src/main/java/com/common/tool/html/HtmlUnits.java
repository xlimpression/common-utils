package com.common.tool.html;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class HtmlUnits {

	private static final int max_table_row_or_column_size = 512;

	private static final String JSOUP_EXPRESSION_SPLITTER = "||";
	private static final String PUBLISH_TIME_JSOUP_EXPRESSION = "#pubTime";
	private static final String PUBLISH_TIME_REGEX = "(\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2})||(\\d{4}年\\d{2}月\\d{2}日  \\d{2}:\\d{2})";


	/**
	 * 
	 */
	public static int convertTableLabelToArray(String table[][], int rowBeginIndex, int columnBeginIndex, int rowEnd,
			int columnEnd, boolean ignoreRowSpan, boolean ignoreColSpan, Element node) {

		checkNotNull(table);
		checkNotNull(node);
		checkArgument(rowBeginIndex >= 0 && rowBeginIndex < max_table_row_or_column_size);
		checkArgument(columnBeginIndex >= 0 && columnBeginIndex < max_table_row_or_column_size);

		Element element = (Element) node;
		int i = 0;
		// rows
		Elements trElements = element.select("tr");
		for(int k = 0; k < trElements.size(); k++) {
			Element trElement = trElements.get(i);
			if(Strings.isNullOrEmpty(trElement.text())) {
				trElements.remove(trElement);
			}
		}

		if (trElements != null && !trElements.isEmpty()) {
			
			for (i = 0; i < trElements.size(); i++) {
				
				
				int rowIndex = rowBeginIndex + i;
				if (rowEnd != -1 && rowIndex > rowEnd) {
					 throw new ArrayIndexOutOfBoundsException("Access out of table range.");
				}
				Element trElement = trElements.get(i);
				Elements parents = trElement.parents();

				// filter nested <tr> label
				boolean flag = parents.stream().anyMatch(parent -> {
					return "tr".equalsIgnoreCase(parent.tagName());
				});
				if (flag) {
					continue;
				}

				// columns
				Elements tdOrTrElements = trElement.select("> td, th");

				for (int j = 0; j < tdOrTrElements.size(); j++) {

					int columnIndex = columnBeginIndex + j;
					while (table[rowIndex][columnIndex] != null) {
						columnIndex++;
					}
					if (columnEnd != -1 && columnIndex > columnEnd) {
						throw new ArrayIndexOutOfBoundsException("Access out of table range.");
					}
					Element tdOrTrElement = tdOrTrElements.get(j);
					Elements parentElements = tdOrTrElement.parents();

					// filter nested <td> or <th> label
					boolean anyMatch = parentElements.stream().anyMatch(parentElement -> {
						return "td".equalsIgnoreCase(parentElement.tagName());
					});

					if (anyMatch) {
						continue;
					}

					// calculate rowspan and colspan
					int rowspan = Optional.ofNullable(tdOrTrElement.attr("rowspan")).filter(StringUtils::isNotBlank)
							.map(HtmlUnits::parseInt).orElse(1);
					int colspan = Optional.ofNullable(tdOrTrElement.attr("colspan")).filter(StringUtils::isNotBlank)
							.map(HtmlUnits::parseInt).orElse(1);

					Elements nestedTables = tdOrTrElement.select("> table");

					if (nestedTables == null || nestedTables.isEmpty()) {
						for (int k = rowIndex; k < rowIndex + rowspan; k++) {
							for (int p = columnIndex; p < columnIndex + colspan; p++) {
								if (ignoreRowSpan && k > rowIndex) {
									table[k][p] = StringUtils.EMPTY;
								} else if (ignoreColSpan && p > columnIndex) {
									table[k][p] = StringUtils.EMPTY;
								} else {
									table[k][p] = Optional.ofNullable(tdOrTrElement.text()).map(String::trim)
											.orElse(StringUtils.SPACE);
									if(StringUtils.EMPTY.equals(table[k][p])) {
										table[k][p] = StringUtils.SPACE;
									}
								}
							}
						}

					} else if (nestedTables != null && !nestedTables.isEmpty()) {
						int q = rowIndex;
						for (Element nestedTable : nestedTables) {
							if (q < rowIndex + rowspan) {
								q = convertTableLabelToArray(table, rowIndex, columnIndex, rowIndex + rowspan - 1,
										columnIndex + colspan - 1, ignoreRowSpan, ignoreColSpan,
										Jsoup.parse(nestedTable.toString()));
							}
						}

						for (int k = rowIndex; k < rowIndex + rowspan; k++) {
							for (int p = columnIndex; p < columnIndex + colspan; p++) {
								table[k][p] = Optional.ofNullable(table[k][p]).orElse(StringUtils.EMPTY);
							}
						}
					}
				}
			}
		}
		return i;
	}
	
	
	// parse integer like 7 or "7" or \"7\"format 
	private static int parseInt(String s) {
		
		checkArgument(!Strings.isNullOrEmpty(s));
		
		s = s.trim();
		if(StringUtils.isNumeric(s)) {
			return Integer.parseInt(s);
		}
		String regex = "^\"[0-9]\\d*\"$";
		if(match(regex, s)) {
			s = s.substring(1, s.length() - 1);
			if(Strings.isNullOrEmpty(s)) {
				throw new  NumberFormatException();
			}
			return Integer.parseInt(s);
		}
		String reg = "^\\\\\"[0-9]\\d*\\\\\"$";
		if(match(reg, s)) {
			s = s.substring(2, s.length() - 2);
			if(Strings.isNullOrEmpty(s)) {
				throw new  NumberFormatException();
			}
			return Integer.parseInt(s);
		}
		
		throw new  NumberFormatException();
	}
	
	public static boolean match(String regex, String s) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(s);
		return matcher.matches();
	}

	private static List<String> convertTableLabelToList(boolean ignoreRowSpan, boolean ignoreColSpan, Element node) {

		checkNotNull(node);

		List<String> tableList = new LinkedList<String>();

		String tableArray[][] = new String[max_table_row_or_column_size][max_table_row_or_column_size];
		int rowsNum = convertTableLabelToArray(tableArray, 0, 0, -1, -1, ignoreRowSpan, ignoreColSpan, node);
		if (rowsNum > 0) {
			for (int i = 0; i < rowsNum; i++) {
				List<String> rowList = new LinkedList<>();
				for (int j = 0; j < max_table_row_or_column_size; j++) {
					if (tableArray[i][j] != null && !StringUtils.EMPTY.equals(tableArray[i][j])) {
						rowList.add(tableArray[i][j]);
					}
				}
				if (!rowList.isEmpty()) {
					tableList.add(Joiner.on("||").skipNulls().join(rowList));
				}
			}
		}
		return tableList;
	}

	public static List<Table> extractTableLabels(boolean ignoreRowSpan, boolean ignoreColSpan, String html) {

		checkArgument(!Strings.isNullOrEmpty(html));
		List<Table> results = new LinkedList<>();

		Document document = Jsoup.parse(html);
		Elements tables = document.select("table");

		if (tables == null || tables.isEmpty()) {
			return results;
		}

		for (int i = 0; i < tables.size(); i++) {

			Table tableRet = new Table();

			Element table = tables.get(i);

			// extract preContent
			Elements siblings = table.previousElementSiblings();
			List<String> collect = siblings.stream().map(item -> {
				return item.text();
			}).filter(item -> {
				return !Strings.isNullOrEmpty(item);
			}).collect(Collectors.toList());
			int size = collect.size();
			if (size >= 1 &&  size <=3) {
				tableRet.setPreContent(collect);
			} else if (collect.size() > 3) {
				tableRet.setPreContent(collect.subList(0, 3));
			}
			if(tableRet.getPreContent() != null) {
				List<String> preContent = tableRet.getPreContent();
				boolean anyMatch = preContent.stream().anyMatch(item -> {
					return Optional.ofNullable(item).orElse(StringUtils.EMPTY).contains("公告概要");
				});
				if(anyMatch) {
					continue;
				}
			}
			
			// table content
			try {
				tableRet.setContent(convertTableLabelToList(ignoreRowSpan, ignoreColSpan, Jsoup.parse(table.toString()))) ;
			}catch (Exception e) {
				// eat exception
				continue;
			}
			results.add(tableRet);
		}
		return results;
		
	}

	public static List<String> getContentAsList(String html, boolean isExcludeTables){
		
		checkArgument(!Strings.isNullOrEmpty(html));
		Document document = Jsoup.parse(html);
		return Splitter.on(StringUtils.LF).
				omitEmptyStrings().
				splitToList(
						buildStringFromNode(document, isExcludeTables).toString().trim().
								replaceAll(StringUtils.SPACE + "+", StringUtils.SPACE)).
				stream().
				map(String::trim).
				filter(item -> {
					return !Strings.isNullOrEmpty(item);
				}).
				collect(Collectors.toList());
	}

	public static List<String> getContentAsList(String html){
		return getContentAsList(html, false);
	}

	public static List<String> getContentAsListExcludeTables(String html){
		return getContentAsList(html, true);
	}

	private static StringBuffer buildStringFromNode(Node node, boolean isExcludeTables) {

		StringBuffer buffer = new StringBuffer();

		// dfs
		if (node instanceof TextNode) {
			// if text node , return 
			TextNode textNode = (TextNode) node;
			buffer.append(textNode.text().trim().replaceAll("[\\s\\p{Zs}]+", StringUtils.EMPTY));
		}
		if(isExcludeTables && node instanceof Element && "table".equalsIgnoreCase(((Element) node).tagName())){
			return buffer;
		}

		// deal with child nodes first
		for (Node childNode : node.childNodes()) {
			buffer.append(buildStringFromNode(childNode, isExcludeTables));
			if("td".equalsIgnoreCase(node.nodeName()) || "th".equalsIgnoreCase(node.nodeName())) {
				buffer.append(StringUtils.SPACE);
			}
		}
		
		// add '\n' to each block label
		if (node instanceof Element) {
			Element element = (Element) node;
			String tagName = element.nodeName();
			if (isBlockLabel(tagName)) {
				if("tr".equals(tagName) || "br".equals(tagName)){
					buffer.append(StringUtils.LF);
				}else{
					boolean flag = false;
					Elements parents = element.parents();
					for(int i = 0; i < parents.size(); i++){
						String parentTagName = parents.get(i).nodeName();
						if(StringUtils.isNotBlank(parentTagName) && ("td".equals(parentTagName) || "th".equals(parentTagName))){
							flag = true;
							break;
						}
					}
					if(!flag){
						buffer.append(StringUtils.LF);
					}
				}
			}
		}
		return buffer;
	}

	/**
	 * judge if a label is a block 
	 */
	private static boolean isBlockLabel(String labelName) {
		
		if(Strings.isNullOrEmpty(labelName)) {
			return false;
		}
		
		if("p".equals(labelName) || 
				"br".equals(labelName) || 
				"div".equals(labelName) ||
				"ul".equals(labelName) || 
				"ol".equals(labelName) || 
				"li".equals(labelName) ||
				"dl".equals(labelName) || 
				"dt".equals(labelName) || 
				"dd".equals(labelName) ||
				"h1".equals(labelName) || 
				"h2".equals(labelName) || 
				"h3".equals(labelName) || 
				"h4".equals(labelName) || 
				"h5".equals(labelName) || 
				"h6".equals(labelName) || 
				"tr".equals(labelName) ||
				"table".equals(labelName) ||
				"caption".equals(labelName) ||
				"form".equals(labelName)){
			return true;
		}
		
		return false;
	}

	public static String extractPublishTime(String html){
		checkArgument(!Strings.isNullOrEmpty(html));
		Document document = Jsoup.parse(html);
		List<String> publicTimeJsoupExpressions = Splitter.on(JSOUP_EXPRESSION_SPLITTER).omitEmptyStrings().splitToList(PUBLISH_TIME_JSOUP_EXPRESSION);
		for(int i = 0; i < publicTimeJsoupExpressions.size(); i++){
			String publicTimeJsoupExpression = publicTimeJsoupExpressions.get(i).trim();
			if(Strings.isNullOrEmpty(publicTimeJsoupExpression)){
				continue;
			}
			Elements publicTimeElements = document.select(publicTimeJsoupExpression);
			if(publicTimeElements != null && !publicTimeElements.isEmpty()){
				for(int j = 0; j < publicTimeElements.size(); j++){
					Element publicTimeElement = publicTimeElements.get(i);
					if(publicTimeElement == null || Strings.isNullOrEmpty(publicTimeElement.text())){
						continue;
					}
					return publicTimeElement.text().trim();
				}
			}
		}
		List<String> publicTimeRegexList = Splitter.on(JSOUP_EXPRESSION_SPLITTER).omitEmptyStrings().splitToList(PUBLISH_TIME_REGEX);
		String content = document.text();
		for(int i = 0; i < publicTimeRegexList.size(); i++){
			String regex = "(" + publicTimeRegexList.get(i) + ")";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(content);
			if(matcher.find()){
				return matcher.group();
			}
		}
		return null;
	}

	/**
	 * judge whether a html contains any table
	 * @param html
	 * @return true only if parameter <code>html</code> contains some table
	 */
	public static boolean isContainedTableInHtmlSource(String html){
		checkArgument(StringUtils.isNotBlank(html));
		Document document = Jsoup.parse(html);

		// first page's table
		Elements tbodyElements = document.select("div.table:contains(公告概要) > table");

		Elements tables = document.select("table");

		// has no table
		if(tables == null || tables.isEmpty()) {
			return false;
		}

		if(tbodyElements != null && !tbodyElements.isEmpty()){
			// filter first page's table
			long count = tables.stream().filter(table -> {
				if (tbodyElements.contains(table)) {
					return false;
				}
				return true;
			}).count();

			if(count == 0){
				return false;
			}else{
				return true;
			}
		}
		return true;
	}


}
