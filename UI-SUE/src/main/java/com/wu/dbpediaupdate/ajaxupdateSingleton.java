package com.wu.dbpediaupdate;

import at.tuwien.dbai.rewriter.Stopwatch;
import org.dbpedia.extraction.WikiDML;
import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.updateresolution.RDFUpdateResolver;
import org.dbpedia.updateresolution.Update;
import org.dbpedia.updateresolution.UpdatePattern;
import org.json.JSONException;
import org.json.JSONObject;
import scala.collection.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("/ajaxquerySingleton")
public class ajaxupdateSingleton {

	@Context
	ServletContext context;

	private static final String PATH_ONTOLOGY = "/WEB-INF/classes/ontology.xml";
	private static final String PATH_DOWNLOADS = "/WEB-INF/downloads";
	private static final String PATH_MAPPINGS = "/WEB-INF/mappings";

	private static final Integer MAX_SAMPLING = 300;

	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String getItHTMLFORM(@FormParam("query") String query,
			@FormParam("stats") String stats,
			@FormParam("sample") String sample,
			@Context HttpServletRequest req,
			@Context HttpServletResponse servletResponse) throws IOException {

		System.out.println("Stats:" + stats);

		Stopwatch watch = new Stopwatch();
		watch.start();
		String localPath = context.getRealPath(PATH_DOWNLOADS);
		String localPathOntology = context.getRealPath(PATH_ONTOLOGY);
		String localPathMappings = context.getRealPath(PATH_MAPPINGS);
		String ret = "";

		String divjsoninfoboxes = "";
		String originalWikiPage = "";
		String title_wiki = "";
		String initial_title_wiki = "";

		//
		/*
		 * String testQuery = //
		 * "prefix owl: <http://www.w3.org/2002/07/owl#> prefix xsd: <http://www.w3.org/2001/XMLSchema#>"
		 * + //"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
		 * //"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+
		 * //"prefix foaf: <http://xmlns.com/foaf/0.1/>"+
		 * //"prefix dc: <http://purl.org/dc/elements/1.1/>"+
		 * "prefix : <http://dbpedia.org/resource/>"+
		 * "prefix dbp: <http://en.dbpedia.org/property/>"+
		 * //"prefix dbpedia: <http://dbpedia.org/>"+
		 * //"prefix skos: <http://www.w3.org/2004/02/skos/core#>"+
		 * //"prefix dbo: <http://dbpedia.org/ontology/>"+
		 * "DELETE { :Santi_Cazorla dbp:position :Midfielder .}"+
		 * "INSERT { :Santi_Cazorla dbp:position :Attacker .}"+ "WHERE"+ "{"+
		 * "}";
		 */
		// InfoboxSandboxCustom info = new InfoboxSandboxCustom(null,"");
		// InfoboxSandboxCustom info = new InfoboxSandboxCustom(null,
		// "_test.xml"); //this worked for checkConsistency
		System.out.println("lets create InfoboxSandboxCustom");
		try {

			HttpSession session = req.getSession(true);
			RDFUpdateResolver resolver = Init.getRDFUpdateResolver();

			/*
			 * Minor check query
			 */
			if (!query.contains("WHERE")) {
				query = query + "\nWHERE{}";
			}
			System.out.println("query:" + query);

			/*
			 * INSERT
			 *
			 * info.getGroundTriplesFromUpdateQuery(query);
			 * scala.collection.mutable
			 * .ArrayBuffer<scala.collection.Seq<WikiDML>> test =
			 * info.resolveForLanguageFromUI();
			 */

			//Tuple2<scala.collection.mutable.ArrayBuffer<scala.collection.mutable.ArrayBuffer<Tuple2<String, scala.collection.mutable.ArrayBuffer<scala.collection.Seq<WikiDML>>>>>, scala.collection.mutable.ArrayBuffer<scala.collection.mutable.ArrayBuffer<Tuple2<String, scala.collection.mutable.ArrayBuffer<scala.collection.Seq<WikiDML>>>>>> update = info
			//		.updateFromUpdateQuery(query);

			Map<String,Set<Update>> update = resolver.updateFromUpdateQuery(query);

			// System.out.println("update:" + update);

			String jscript = "";

			int w = 0;

			ret += "<div id=\"alternativesSubjects\">";
			int i = 0;

			String insertPropertyDbpedia = null;
			int numTabsVertical = 1;
			for(String subject : update.keySet()) { // iterate on pages

				title_wiki = subject;

				// int i = 0;
				ret += "<div id=\"subjectstab-" + (w + 1) + "\">";
				if (!update.get(subject).isEmpty() ) {

					/*
					 * GET infobox page for the text comparison
					 */
					try {

						// get JSON
						// in the Web browser, change format to jsonfm for a
						// pretty version
						// JSONObject json =
						// readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles=Santi_Cazorla&rvsection=0");
						JSONObject json = readJsonFromUrl("https://en.wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles="
								+ title_wiki + "&rvsection=0");

						JSONObject jsonPage = json.getJSONObject("query")
								.getJSONObject("pages");
						String page = jsonPage.toString();
						String[] names = JSONObject.getNames(jsonPage);
						if (names.length > 0) {
							String nameAtt = names[0];
							page = jsonPage.getJSONObject(nameAtt)
									.getJSONArray("revisions").getJSONObject(0)
									.get("*").toString();
						}

						originalWikiPage = page.replace("\n", "<br/>");
						/*
						 * .replace("| position ",
						 * "<span style=\"background-color: #FFFF00\">| position </span>"
						 * );
						 */
						// getJSONArray("revisions").getJSONObject(0).get("*").toString().replace("\n",
						// "<br><br/>"));
					} catch (Exception e) {
						originalWikiPage = "<span style=\"background-color: #FFFF00\">Content is temporarily unavailable</span>";
					}
					// divjsoninfoboxes+=originalWikiPage;

					ret += "<div id=\"tpsSubject\">";
					String retTemp = "";
					boolean firstTP = true;

					ret += "<a id=\"showtp-" + numTabsVertical
						+ "\" style=\"cursor:pointer;";
					if (firstTP)
						ret += "font-weight:700";
					ret += "\"> <img src=\"img/click.jpg\" width=5% style=\"vertical-align: middle;\">"
							+ escapeHtml("ORIIIGINAL UPDATE (originalUpdate)") + "</a><br/>";
					if (!firstTP)
						ret +="<br/>";
					retTemp += "<div id=\"tpHide-" + numTabsVertical + "\"";
					if (firstTP)
						firstTP = false;
					else
						retTemp += " style=\"display:none\"";
					retTemp += ">";

					retTemp += "<div id=\"alternativesDML\">";
					retTemp += "<div id=\"tabsvertical-"
							// + (w + 1)
							+ numTabsVertical
							+ "\" class=\"ui-tabs-verticalsmall ui-helper-clearfix\">";

					numTabsVertical++;
					// FIXME try to update
					String tempRetOptions = "<div id=\"tabsOptionsContent\">";
					int initialIndex = i;
					int numoption = 0;
					ArrayList<Seq<WikiDML>> seqsampling = new ArrayList<Seq<WikiDML>>();
					for (Update u : update.get(subject) ) { // iterate on options
						numoption++;
						ArrayList<String> tempInsertProperties = new ArrayList<String>();
						ArrayList<String> tempInsertValues = new ArrayList<String>();
						ArrayList<String> tempDelProperties = new ArrayList<String>();
						ArrayList<String> tempDelValues = new ArrayList<String>();

						String addsWikipedia = u.toString(UpdatePattern.WIKI_INSERT());
						String delsWikipedia = u.toString(UpdatePattern.WIKI_DELETE());

						String exportDMLs = "";

						// testonly, add deletion to test it
						/*
						 * String testonly =
						 * "ON wikiPage = Santi_Cazorla\nINSERT InfoboxTemplate(infobox football biography).name = \"Santi Cazorla\";"
						 * ;
						 * tempDelProperties.add(getWikiProperty(testonly));
						 * tempDelValues.add(getWikiValue(testonly));
						 */

						// System.out.println("end iterate");
						tempRetOptions += "<div id=\"tabs-" + (i + 1)
								+ "\">";
						// ret += "<div id=\"test-" + (i + 1) +
						// "\">Test</div>";

						// System.out.println("call wikiResult:");
						tempRetOptions += wikiResult();
						// System.out.println("call startTable:");
						tempRetOptions += startTable();
						// System.out.println("call printRow gree:");
						tempRetOptions += printRow(addsWikipedia, "green");
						// System.out.println("call printRow red:");
						tempRetOptions += printRow(delsWikipedia, "red");
						// System.out.println("call closeTable:");
						tempRetOptions += closeTable();

						// System.out.println("call the diff:");
						// System.out.println("seq:"+seq);
						String addsDbpedia = "";
						String delsDbpedia = "";


						tempRetOptions += DBpediaResult();
						tempRetOptions += startTable();
						tempRetOptions += printRow(addsDbpedia, "green");
						tempRetOptions += printRow(delsDbpedia, "red");
						tempRetOptions += closeTable();
						tempRetOptions += "<div style=\"padding-top:10px;width:100%; text-align: left\"><a id=\"mailFeedback\" href=\"mailto:dbpediaupdate@ai.wu.ac.at?subject=feedback on query&body=Hi, please check the query:\n\n"
								+ escapeHtml(query)
								+ "\" title=\"Report on query results\"><img src=\"img/mail.png\" style=\"padding-right:10px;\" width=50px>Report on query results</a></div>";

						tempRetOptions += "<div id=\"consistencywikiDMLs-"
								+ (i + 1)
								+ "\" style=\"text-align:right\"> Mark to check consistency <input type=\"checkbox\" name=\"wikidml\" value=\"wikiDMLs-"
								+ (i + 1) + "\"";
						if (numoption == 1) {
							tempRetOptions += " checked";
						}

						tempRetOptions += "><div style=\"display:none\" id=\"wikiDMLs-"
								+ (i + 1)
								+ "\">"
								+ exportDMLs
								+ "</div></div>";

						tempRetOptions += "</div>"; // close tabs-"+i

						/*
						 * Apply wiki page changes
						 */
						String currentWikiPage = originalWikiPage;

						// insert new properties
						int posInfobox = currentWikiPage
								.indexOf("{{Infobox");
						String preInsert = currentWikiPage.substring(0,
								currentWikiPage.indexOf("|", posInfobox));
						// System.out.println("preInsert:" + preInsert);
						String postInsert = currentWikiPage
								.substring(currentWikiPage.indexOf("|",
										posInfobox));
						// System.out.println("postInsert:" + postInsert);
						String insertlines = "";


						for (int j = 0; j < tempInsertProperties.size(); j++) {

							insertlines += "<span style=\"background-color: green;\">"
									+ "| "
									+ tempInsertProperties.get(j)
									+ " = "
									+ tempInsertValues.get(j).replace("\"",
											"") + "</span>" + "<br>";
						}
						currentWikiPage = preInsert + insertlines
								+ postInsert;
						// System.out.println("currentWikiPage:"+currentWikiPage);

						// TODO DEBUG
						// tempDelProperties.add("name");
						// delete properties

						for (int j = 0; j < tempDelProperties.size(); j++) {
							String valueNoQuotes = tempDelValues.get(j)
									.replace("\"", "");
								// <br/>
							String tooltip = "<span style=\"background-color: red;\" title=\""
									+ tempDelProperties.get(j)
									+ " = "
									+ valueNoQuotes
									+ "\">&nbsp;&nbsp;&nbsp;</span><br\\/>";
							currentWikiPage = currentWikiPage.replaceAll(
									"\\| " + tempDelProperties.get(j)
											+ " += " + valueNoQuotes
											+ "<br\\/>", tooltip); // +=
																	// escape
																	// one
																	// or
																	// more
																	// spaces
																	// escape
																	// /
																	// of
																	// <br/>

						}
						System.out.println("currentWikiPage after:"
								+ currentWikiPage);

						if (i == 0) {
							divjsoninfoboxes += "<div id=\"jsoninfobox-"
									+ (i + 1) + "\">";
						} else {
							divjsoninfoboxes += "<div id=\"jsoninfobox-"
									+ (i + 1)
									+ "\" style=\"display:none\">";
						}
						divjsoninfoboxes += "<img style=\"cursor:pointer;\" title=\"Copy text to your clipboard\" width=30px align=\"middle\" src=\"img/copy.png\" id=\"copyButton-"
								+ (i + 1)
								+ "\"><span id=\"msg-"
								+ (i + 1)
								+ "\"></span><br/>";
						divjsoninfoboxes += currentWikiPage + "</div>";

						i++;
					}

					tempRetOptions += "</div>"; // close tabsOptionsContent

					// <div id=\"chart_div\"></div>

					if (stats != null && stats.equalsIgnoreCase("true")
							&& insertPropertyDbpedia != null) {

						int sampling = 100;
						if (sample != null) {
							try {
								sampling = Integer.parseInt(sample);
								if (sampling > MAX_SAMPLING) {
									sampling = MAX_SAMPLING;
								}
							} catch (NumberFormatException e) {
								sampling = 100;
							}
						}
						scala.collection.mutable.HashMap<String, Object> statOutput = new scala.collection.mutable.HashMap<>();
						// FIXME this has to be changed to work with
						// Arraylist
						Seq<Seq<WikiDML>> seqsamplingStats = JavaConversions.asScalaBuffer(seqsampling).toSeq();


						  statOutput = resolver.getStatResultsAlternatives(
						  "<http://dbpedia.org/resource/" + title_wiki +
						  ">", insertPropertyDbpedia, sampling, seqsamplingStats);


						retTemp += "<div style=\"margin-bottom:10px\">";
						// <a
						// id=\"showstats\" style=\"cursor:pointer\">Show/Hide Stats</a>";
						System.out.println(statOutput);

						if (statOutput.size() > 0) {
							scala.collection.Iterator<String> itprops = statOutput
									.keysIterator();
							retTemp += "<table id=\"statsTable\" style=\"margin-left:150px;\"><tr><td colspan=2 bgcolor=lightgrey> Presence of alternative wikipedia properties producing '"
									+ insertPropertyDbpedia + "'</td><tr>";

							while (itprops.hasNext()) {
								String props = itprops.next();
								Integer value = Integer.parseInt(statOutput
										.get(props).toString()
										.replace("(", "").replace(")", "")
										.replace("Some", ""));
								// erase the infobox info
								props = props
										.substring(props.indexOf(':') + 1);
								int length = 0;
								String per = "0 %";
								if (value != 0) {
									length = 400 * (sampling / value);
									int percentage = (sampling / value) * 100;
									per = percentage + " %";
								}

								retTemp += "<tr><td>"
										+ props
										+ ":</td><td><table><tr><td bgcolor=\""
										+ getRandomColor()
										+ "\" height=\"10px\" width=\""
										+ length + "px\" title=\"" + per
										+ "\"></td></tr></table></td></tr>";
							}
							retTemp += "</table>";
						}

						// ret+=statOutput.toString();
						retTemp += "</div>";
					}
					/*
					 * Write the name of the tabs
					 */
					retTemp += "<div id=\"tabsOptionsmenu\">";
					retTemp += "<ul id=\"tabsnum\">";

					for (int n = initialIndex; n < i; n++) {
						retTemp += "<li><a id=\"tabselect-" + (n + 1)
								+ "\" href=\"#tabs-" + (n + 1) + "\">OPT#"
								+ (n + 1) + "</a></li>\n";
					}
					retTemp += "</ul>"; // close tabsNum

					retTemp += "</div>"; // close tabsOptionsmenu

					retTemp += tempRetOptions;
					retTemp += "</div>"; // close tabsvertical

					retTemp += "</div>"; // close alternativesDML
					retTemp += "</div>"; // close tpHide


					ret += retTemp + "</div>"; // close tpsSubject;

				}

				ret += "</div>"; // close subjectstab

				w += 1;
			}

			/*
			 * END OF ITERATION TRIPLES
			 */
			ret += "<div style=\"text-align:right\"><button id=\"checkconsistency\">Check consitency of selected results</button><div id=\"resultConsistency\" style=\"margin-top:10px\"></div></div>";

			ret += "</div>"; // close alternativesTriples
			/*
			 * Write the name of the tabs
			 */
			ret += "<ul id=\"tabsTriples\">";

			for (int n = 1; n <= w; n++) {
				java.util.Iterator<String> kit = update.keySet().iterator();
				String title = "TITLE MISSING";
				if( kit.hasNext() ) {
					title =	kit.next();
				}
				ret += "<li><a id=\"tabtriplesselect-" + n
						+ "\" href=\"#subjectstab-" + n + "\">"
						+ title + "</a></li>\n";
			}
			ret += "</ul>"; // close tabsNum

			/*
			 * print infoboxes
			 */
			ret += "<div id=\"jsoninfoboxes\">" + divjsoninfoboxes + "</div>";

			ret += "<div id=\"js2\" style=\"display:none;\" >"
			// + "<script id=\"js\">\n"
					+ "$(document).ready(function() {\n";
			// ret+="alert(\"test\");\n";

			// for (int p = 1; p <= w; p++) {
			for (int p = 1; p <= numTabsVertical; p++) {
				jscript += "$(\"#tabsvertical-" + p + "\" ).tabs();\n";
				System.out
						.println("\n*****************Doing tabs: " + p + "\n");

				// add show/hide functionality
				jscript += "document.getElementById(\"showtp-" + p
						+ "\").addEventListener(\"click\", function() {\n";
				jscript += "$('#showtp-" + p
						+ "').css( 'font-weight', '700' );";
				for (int k = 1; k <= numTabsVertical; k++) {
					if (k != p) {
						jscript += "$(\"#tpHide-" + k + "\").hide();\n";
						jscript += "$('#showtp-" + k
								+ "').css( 'font-weight', '400' );";
					}
				}
				jscript += "$(\"#tpHide-" + p + "\").show();\n" + "});\n";
			}
			System.out.println("jscript so far:" + jscript);

			/*
			 * UPDATE SCRIPT
			 */
			for (int n = 1; n <= i; n++) {
				jscript += "document.getElementById(\"copyButton-"
						+ n
						+ "\").addEventListener(\"click\", function() {"
						+ "copyToClipboardMsg(document.getElementById(\"jsoninfobox-"
						+ n + "\"), \"msg-" + n + "\");});\n";

				jscript += "$(\"#tabselect-" + n + "\").click(function(){\n";
				// jscript+="alert(\"visible!\");\n";
				for (int k = 1; k <= i; k++) {
					if (k != n)
						jscript += "$(\"#jsoninfobox-" + k + "\").hide()\n";
				}
				jscript += "$(\"#jsoninfobox-" + n + "\").show()\n" + "});\n";
				/*
				 * jscript+="$('#infobox_text').html('tabs-"+(i+1)+"');\n" +
				 * "});\n";
				 */

				// for the visibility of the stats
				// jscript+="$( \"#showstats\" ).click(function() {\n"+
				// "$( \"#statsTable\").toggle();\n"+
				// "});\n";
			}

			ret += jscript;
			ret += "});\n"
					+ "</div>";

			ret += "<div id=\"title_wiki\">" + title_wiki + "</div>";
			System.out.println("ret:" + ret);

		} catch (Exception e) {
			System.out.println("except:" + e);
			e.printStackTrace(System.out);
		}

		watch.stop();
		System.out.println("Time:" + watch.getTime());

		return ret;
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException,
			JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			// System.out.println("jsontext>" + jsonText);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	private String startTable() {
		String ret = "<table><tr>" + "<td><h3>ADDs</h3></td>"
				+ "<td><h3>DELETEs</h3></td>" + "</tr><tr>";
		return ret;
	}

	private String closeTable() {
		return "</tr></table>";
	}

	private String wikiResult() {
		String ret = "";
		ret += "<h2><img src=\"img/wikipedia_logo_detail.gif\" width=\"75px\" align=\"middle\" style=\"padding-right: 10px\"></img>WIKIPEDIA RESULTS</h2>";
		return ret;
	}

	private String DBpediaResult() {
		String ret = "";
		// <div style=\"margin-top: 50px;\">
		ret += "<h2><img src=\"img/dbpedia.png\" width=\"75px\" align=\"middle\" style=\"padding-right: 10px\"></img>DBPEDIA RESULTS</h2>";

		return ret;
	}

	private String printRow(String values, String color) {
		// #73AD21
		String ret = "<td valign=\"top\"><div style=\"border-radius: 25px; border: 2px solid "
				+ color
				+ "; padding: 20px; width: 350px; min-height: 150px; word-break: break-all; white-space: normal;\">";
		ret += values;
		ret += "</div></td>";
		return ret;
	}

	public String parseQuad(Quad quad) {
		// System.out.println("my quad is:" + quad);
		String ret = "<" + quad.subject() + "> <" + quad.predicate() + "> ";
		// add quotes if needed
		if (((quad.language() != null) || (quad.datatype() != null))
				&& (!quad.value().startsWith("\""))) {
			ret += "\"" + quad.value() + "\"";
		} else
			ret += quad.value();
		if (quad.language() != null) {
			ret += "@" + quad.language();
		} else if (quad.datatype() != null) {
			ret += "^^<" + quad.datatype() + ">";
		} // according to
			// https://www.w3.org/TR/turtle/#grammar-production-RDFLiteral,
			// language and datatype are mutually exclusive.
		ret += " .\n";
		return escapeHtml(ret);

	}

	public String getWikiPropertyFromWikiDML(String line) {
		int posPar = line.indexOf(").");
		return line.substring(posPar + 2, line.indexOf(" =", posPar));
	}

	public String getWikiValueFromWikiDML(String line) {
		int posPar = line.indexOf(").");
		return line
				.substring(line.indexOf("= ", posPar) + 2, line.length() - 2); // -2
																				// to
																				// get
																				// rid
																				// of
																				// last
																				// ';'
	}

	public String getRandomColor() {
		Random rand = new Random();
		float r = rand.nextFloat();
		float g = rand.nextFloat();
		float b = rand.nextFloat();
		// Color randomColor = new Color(r, g, b);

		return "rgb(" + r + "," + g + "," + b + ")";
	}
}