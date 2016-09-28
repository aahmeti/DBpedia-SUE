<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></meta>
<title>DBpedia-SUE -- The DBpedia SPARQL Update Endpoint</title>



<style type="text/css">
/*<![CDATA[*/
html {
	padding: 0;
}

body {
	padding: 0;
	margin: 0;
	font-family: Arial, Helvetica, sans-serif;
	font-size: 9pt;
	color: #333;
	/*background-color: #FDFDFD;*/
	background-image: url("img/gradient_Dbpedia.png");
	/* change, propietary*/
}

#header {
	padding: 0;
	margin: 0;
	background-color: #FF8000;
	color: #FFFFFF;
	border-bottom: 1px solid #AAA;
}

#header h1 {
	font-size: 16pt;
	font-weight: normal;
	text-align: left;
	vertical-align: middle;
	padding: 4px 8px 4px 8px;
	margin: 0px 0px 0px 0px;
}

#menu {
	margin-left: 8px;
	margin-right: 8px;
	margin-top: 0px;
	clear: right;
	float: right;
}

#intro, #main {
	margin-left: 8px;
	margin-right: 8px;
}

#help {
	margin-left: 8px;
	margin-right: 8px;
	width: 80%
}

#footer {
	width: 100%;
	float: left;
	clear: left;
	margin: 2em 0 0;
	padding-top: 0.7ex;
	border-top: 1px solid #AAA;
	font-size: 8pt;
	text-align: center;
}

fieldset {
	border: 0;
	padding: 0;
	margin: 0;
}

fieldset label {
	font-weight: normal;
	white-space: nowrap;
	font-size: 11pt;
	color: #000;
}

fieldset label.n {
	display: block;
	vertical-align: bottom;
	margin-top: 5px;
	width: 160px;
	float: left;
	white-space: nowrap;
}

fieldset label.n:after {
	content: ":";
}

fieldset label.n1 {
	display: block;
	vertical-align: bottom;
	margin-top: 5px;
	width: 160px;
	float: left;
	white-space: nowrap;
}

fieldset label.ckb {
	width: 160px;
	font-weight: normal;
	font-size: 10pt;
}

fieldset label.ckb:after {
	content: "";
}

fieldset textarea {
	width: 99%;
	font-family: monospace;
	font-size: 10pt;
}

#cxml {
	clear: both;
	display: block;
}

#savefs {
	clear: both;
	display: block;
}

span.info {
	font-size: 9pt;
	white-space: nowrap;
	height: 2em;
}

br {
	clear: both;
}

.infobox {
	margin-left: auto;
	margin-right: auto;
}
/*]]>*/
</style>

<!--  copy clipboard -->
<script
	src="js/copy.js"></script>


<!--  Show/Hide from http://www.w3schools.com/jquery/tryit.asp?filename=tryjquery_hide_slow -->
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js"></script>
<script>
$(document).ready(function(){
    $("#showinfobox").click(function(){
        $("#infobox").show(1000);
         $("#hideinfobox").show();
          $("#showinfobox").hide();
    });
     $("#hideinfobox").click(function(){
        $("#infobox").hide(1000);
         $("#hideinfobox").hide();
          $("#showinfobox").show();
    });
});
</script>
<script>
  $(function() {
    $( "#tabs" ).tabs();
   // $( "#tabsverticalTriples" ).tabs();
    // $( "#tabsvertical" ).tabs();
    //$( "#tabsvertical li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
  });
  </script>
<script id="js">
	$(document)
			.ready(
		
      
					function() {
						
						$('#formSearch--UNCOMMENTTHIS')
								.submit(
										function() { // catch the form's submit event
										//alert("cmon");
										// dialog.dialog( "open" );
										 //clear previous tabs just in case --> fist create, then destroy
										 
									///	$( "#tabsvertical" ).tabs();
									///	$( "#tabsvertical" ).tabs( "destroy" );
										
										$( "#tabsverticalTriples" ).tabs();
										$( "#tabsverticalTriples" ).tabs( "destroy" );
									
						
									///	$('#tabscontent').html("");
									///	$('#tabsmenu').html("");
										
										$('#tabscontentTriples').html("");
										$('#tabsmenuTriples').html("");
										
										$("#results").hide(500);
										$('#infobox_text').html();
										var req =  $
													.ajax({ // create an AJAX call...
														data : $(this)
																.serialize(), // get the form data
														type : $(this).attr(
																'method'), // GET or POST
														url : $("#valueAction").val(), // the file to call, previously taken from  $(this).attr('action') 
														 beforeSend: function(){
														   $("#dialog").dialog('open');
														   //.html("<p>Please Wait...</p>");
														 },
														success : function(
																response) { // on success..
														 	$("#dialog").dialog('close');
															//alert('ok! '+ status);
															$("#results").show(1000);
													//	$('#infobox_text').html(response); // update the DIV
														$('#infobox_text').html($($.parseHTML(response)).filter("#jsoninfoboxes"));
														
														
														
												///		$('#tabsmenu').html($($.parseHTML(response)).filter("#tabsnum"));
														
														$('#tabsmenuTriples').html($($.parseHTML(response)).filter("#tabsTriples"));
														
														
														// this works but prevent to reuse in several calls
														// $( "#tabsvertical" ).append($($.parseHTML(response)).filter("#alternativesDML"));
												///		$('#tabscontent').html($($.parseHTML(response)).filter("#alternativesDML"));
														
														$('#tabscontentTriples').html($($.parseHTML(response)).filter("#alternativesTriples"));
														
												///		 $( "#tabsvertical" ).tabs();
												///		  $( "#tabsvertical li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
														  
														   $( "#tabsverticalTriples" ).tabs();
														  $( "#tabsverticalTriples li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
														
														//$('#infobox_html').load('http://cors.io/?u=https://en.wikipedia.org/wiki/Santi_Cazorla .infobox');
														var title = $($.parseHTML(response)).filter("#title_wiki").html();
														$('#infobox_html').load("http://cors.io/?u=https://en.wikipedia.org/wiki/"+title+" .infobox");
														// clearTimeout( progressTimer );
													     
													      $( "#progressbar" ).progressbar( "value", false );
													      $( ".progress-label" )
													        .text( "Starting update..." );
													        
													      
													        //get the scripts
														//$('#alternativesDML js').each(function (index, element) { eval(element.innerHTML); }); 
													     
															
															//test
															
														//	$( "#tabsvertical" ).append($($.parseHTML(response)).filter("#js2"));
															$( "#tabsverticalTriples" ).append($($.parseHTML(response)).filter("#js2"));
															$('#js2').each(function (index, element) { eval(element.innerHTML); });
															
															addButtonConsistency();
															clearTimeout(progressTimer);
														},
														error : function(xhr,
																status) {
															if (status !='abort') {
																alert('Error! '
																	+ status);
															}
															clearTimeout(progressTimer);
															   $( "#progressbar" ).progressbar( "value", false );
													      $( ".progress-label" )
													        .text( "Starting update..." );
															
														}
													});
											
											 $('#dialog').dialog({
											      title: 'Starting update...',
											      position: { my: "center top", at: "center", of: "#showMessage" },
											      bgiframe: true,
											      modal: true,
											      buttons: {
											        Cancel: function() {
											          if (req){
											            req.abort();
											              $("#dialog").dialog('close');
											             }
											        }
											      }
											    });
											return false; // cancel original event to prevent form submitting
										});	
										
				});
				
		function addButtonConsistency(){
				$( "#checkconsistency").button().on( "click", function() {
				var data = [];
					$("input:checked").each(function() {
					//	alert($("#"+$(this).val()).html());
					  data.push($("#"+$(this).val()).html());
					});
        			// alert(data);
        	
        	var dataobject = {
		            postvar: data
		        } ;
        
        		var test="pp";
        		var req =  $.ajax({ // create an AJAX call...
							//	data :data,
							data:dataobject,
								type : "POST", // GET or POST
								url : "webapi/ajaxcheckconsistency",  
								 
								success : function(
										response) { // on success..
								
								 //alert(response);
								alert("This update is consistent!");
								//	alert('ok! '+ status);
									
								},
								error : function(xhr,
										status) {
									if (status !='abort') {
										alert('Error! '
											+ status);
									}
									
									
								}
							});
				return false; // cancel original event to prevent form submitting	
				});	
        		
        		
        }

</script>
<!--  Progress Bar from https://jqueryui.com/progressbar/#download  -->
<link rel="stylesheet" href="css/jquery-ui.css"></link>
<script src="js/jquery-1.10.2.js"></script>
<script src="js/jquery-ui.js"></script>
<link rel="stylesheet" href="css/style.css"></link>
<script>
 var progressTimer = null;
  $(function() {
   // var progressTimer,
     var progressbar = $( "#progressbar" ),
      progressLabel = $( ".progress-label" ),
      dialogButtons = [{
        text: "Cancel Update",
        click: closeDownload
      }],
      dialog = $( "#dialog" ).dialog({
        autoOpen: false,
        closeOnEscape: false,
        resizable: false,
        buttons: dialogButtons,
        open: function() {
          progressTimer = setTimeout( progress, 2000 );
        },
        beforeClose: function() {
          downloadButton.button( "option", {
            disabled: false,
            label: "Start Update"
          });
        }
      }),
      downloadButton = $( "#runquery" )
        .button()
        .on( "click", function() {
          $( this ).button( "option", {
            disabled: true,
            label: "Updating..."
          });
          dialog.dialog( "open" );
        });
 
    progressbar.progressbar({
      value: false,
      change: function() {
        progressLabel.text( "Current Progress: " + progressbar.progressbar( "value" ) + "%" );
      },
      /*complete: function() {
        progressLabel.text( "Complete!" );
        dialog.dialog( "option", "buttons", [{
          text: "Close",
          click: closeDownload
        }]);
        $(".ui-dialog button").last().focus();
          $("#results").show(1000);
      }*/
    });
 
    function progress() {
      var val = progressbar.progressbar( "value" ) || 0;
 
      progressbar.progressbar( "value", val + Math.floor( Math.random() * 3 ) );
 
      if ( val <= 99 ) {
        progressTimer = setTimeout( progress, 100 );
      }
    }
 
    function closeDownload() {
      clearTimeout( progressTimer );
      dialog
        .dialog( "option", "buttons", dialogButtons )
        .dialog( "close" );
      progressbar.progressbar( "value", false );
      progressLabel
        .text( "Starting update..." );
      downloadButton.focus();
    }
  });
  </script>
<style>
.ui-widget-overlay {
	position: fixed;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
}

.ui-front {
	z-index: 100;
}

.ui-tabs-vertical {
	width: 65em;
}

.ui-tabs-verticalsmall{
	width: 58em;
}

.ui-tabs-vertical .ui-tabs-nav {
	padding: .2em .1em .2em .2em;
	float: left;
}

.ui-tabs-vertical .ui-tabs-nav li {
	clear: left;
	width: 100%;
	border-bottom-width: 1px !important;
	border-right-width: 0 !important;
	margin: 0 -1px .2em 0;
}

.ui-tabs-vertical .ui-tabs-nav li a {
	display: block;
}

.ui-tabs-vertical .ui-tabs-nav li.ui-tabs-active {
	padding-bottom: 0;
	padding-right: .1em;
	border-right-width: 1px;
}

.ui-tabs-vertical .ui-tabs-panel {
	padding: 1em;
	float: left;
	width: 45em;
}
</style>
<style>
#progressbar {
	margin-top: 20px;
}

.progress-label {
	font-weight: bold;
	text-shadow: 1px 1px 0 #fff;
}

.ui-dialog-titlebar-close {
	display: none;
}
</style>
</head>
<body>
	<div id="header">
		<h1>DBpedia-SUE: The DBpedia SPARQL Update Endpoint</h1>
	</div>

	<div id="menu">
		<a href="">About</a> | <a href="">Semantics of SPARQL Updates</a> | <a
			href="">Contact</a>
	</div>

	<div id="main">
	<div id="showMessage"></div>
		<div id="dialog" title="Updating">
			<div class="progress-label">Starting update...</div>
			<div id="progressbar"></div>
		</div>
		<br></br>
		<center>
			<img src="img/dbpediaUpdate.png" width=200px></img>
		</center>
		
		  Examples:
                      <!--  <a style="cursor:pointer;" onclick="document.getElementById('query').value=document.getElementById('example1').innerHTML.replace(/lt;/g, '<').replace(/gt;/g, '>') ;">Change name of soccer Player</a>-->
| <a style="cursor:pointer;" onclick="document.getElementById('query').value=document.getElementById('example2').innerHTML.replace(/lt;/g, '<').replace(/gt;/g, '>') ;">Change population of a city</a>
| <a style="cursor:pointer;" onclick="document.getElementById('query').value=document.getElementById('example3').innerHTML.replace(/lt;/g, '<').replace(/gt;/g, '>') ;">Change resources with a WHERE clause</a>
           <br/><br/>
                        <textarea style="display:none;" name="example1" id="example1" value="ex1">prefix : lt;http://dbpedia.org/resource/gt;
prefix foaf: lt;http://xmlns.com/foaf/0.1/gt;


INSERT { :Cristiano_Ronaldo foaf:name "El Bicho" . }
</textarea>
 <textarea style="display:none;" name="example2" id="example2" value="ex2">prefix : lt;http://dbpedia.org/resource/gt;
prefix dbo: lt;http://dbpedia.org/ontology/gt;

INSERT { :Vienna dbo:populationTotal 1840573 . }
</textarea>
 <textarea style="display:none;" name="example2" id="example3" value="ex3">prefix : lt;http://dbpedia.org/resource/gt;
prefix foaf: lt;http://xmlns.com/foaf/0.1/gt;

INSERT { ?a foaf:name "El Bicho" . }
WHERE {?a foaf:name "Santi Cazorla"@en . }
</textarea>
<form id="formSearch" method="get" action="webapi/get">
			<input type="text" style="display: none;" name="valueAction"
				id="valueAction" value="webapi/get"></input>
			<fieldset>
				<label for="default-graph-uri">Data Set Name (Graph
					IRI)</label><br> <input type="text" name="default-graph-uri"
					id="default-graph-uri" value="http://dbpedia.org" size="80" disabled></input>
				<br></br> <br></br> <label for="query">Query Text</label><br></br>
				<textarea rows="10" cols="80" name="query" id="query">prefix : &lt;http://dbpedia.org/resource/&gt;
prefix foaf: &lt;http://xmlns.com/foaf/0.1/&gt;


INSERT { :Cristiano_Ronaldo foaf:name "El Bicho" . }    			</textarea>
				<!-- prefix : &lt;http://dbpedia.org/resource/&gt;
prefix dbo: &lt;http://en.dbpedia.org/ontology/&gt;

DELETE { :Santi_Cazorla dbo:position :Midfielder .}
INSERT { :Santi_Cazorla dbo:position :Attacker .}
WHERE{}
				 -->
				<br>
				<!--  <span class="info"><i>(Security restrictions of this server do not allow you to retrieve remote RDF data, see <a href="http://dbpedia.org/sparql?help=enable_sponge">details</a>.)</i></span>
		<br>-->
				<label for="format" class="n">Choose Semantics</label> <select
					name="format" id="format" onchange="format_change(this)">
					<option value="auto">Auto</option>
					<option value="brave" selected="selected">Brave</option>
					<option value="cautious">Cautious</option>
					<option value="fainthearted">Fainthearted</option>
				</select> 
				<!--  <br></br> <label class="n" for="options">Options</label>
				<fieldset id="options">
					<input name="debug" id="debug" type="checkbox" checked="checked"></input>
					<label for="debug" class="ckb">XXXXXXX</label>
				</fieldset>
-->
				<br></br> <input id="submit" name="submit"
					title="Write your input query and Run the query" type="submit"
					value="Run Query" /></input>
				<!--<button id="runquery">Run Query</button>-->
				<input type="reset" value="Reset"></input>
			</fieldset>
		</form>

	</div>
	<hr></hr>
	<div style="background-color: white;">

		<div id="results" style="display: none;">
			<h1>Results</h1>


			<table>
				<tr>
					<td valign="top">
						<div id="tabsverticalTriples" class="ui-tabs-vertical ui-helper-clearfix">
							<div id="tabsmenuTriples">
								<!--  <ul>
									<li><a href="#tabsvertical">TP#1</a></li>
							</ul>-->
							</div>
							<div id="tabscontentTriples">
							<div id="tabsvertical" class="ui-tabs-vertical ui-helper-clearfix">
								<div id="tabsmenu"></div>
								<div id="tabscontent"></div>
								<!--  		<ul>
						<li><a href="#tabs-1">OPT#1</a></li>
						<li><a href="#tabs-2">OPT#2</a></li>
						<li><a href="#tabs-3">OPT#3</a></li>
					</ul>-->
								<!-- <div id="tabs-1">
						<div>
	
	
							<h2>
								<img src="img/wikipedia_logo_detail.gif" width="75px"
									align="middle" style="padding-right: 10px"></img>WIKIPEDIA
								RESULTS
							</h2>
							<table>
								<tr>
									<td>
										<h3>ADDs</h3>
									</td>
									<td>
										<h3>DELETEs</h3>
									</td>
								</tr>
								<tr>
									<td>
										<div
											style="border-radius: 25px; border: 2px solid #73AD21; padding: 20px; width: 400px; height: 250px;">
											property = "managerYears", newValue="Arsenal", <br />
											property = "managerYears", newValue="Arsenal", <br />
											property = "managerYears", newValue="Arsenal", <br />
											property = "managerYears", newValue="Arsenal",
										</div>
									</td>
									<td>
										<div
											style="border-radius: 25px; border: 2px solid red; padding: 20px; width: 400px; height: 250px;">
											property = "playerYears", newValue="Arsenal", <br /> property
											= "playerYears", newValue="Arsenal", <br /> property =
											"playerYears", newValue="Arsenal", <br /> property =
											"playerYears", newValue="Arsenal",
	
										</div>
									</td>
	
	
	
								</tr>
							</table>
	
							<div style="margin-top: 50px;">
								<h2>
									<img src="img/dbpedia.png" width="75px" align="middle"
										style="padding-right: 10px"></img>DBPEDIA RESULTS
								</h2>
								<table>
									<tr>
										<td>
											<h3>ADDs</h3>
										</td>
										<td>
											<h3>DELETEs</h3>
										</td>
									</tr>
									<tr>
										<td>
											<div
												style="border-radius: 25px; border: 2px solid #73AD21; padding: 20px; width: 400px; height: 250px;">
												:Thierry_Henry rdf:type
												&lt;http://dbpedia.org/ontology/manager&gt; .</div>
										</td>
										<td>
											<div
												style="border-radius: 25px; border: 2px solid red; padding: 20px; width: 400px; height: 250px;">
												:Thierry_Henry rdf:type
												&lt;http://dbpedia.org/ontology/player&gt; . <br />
												:Thierry_Henry rdf:type
												&lt;http://dbpedia.org/ontology/Athlete&gt; .
	
											</div>
										</td>
									</tr>
								</table>
							</div>
						</div>
					</div>
					<div id="tabs-2">
						<h2>Content heading 2</h2>
						<p>Morbi tincidunt, dui sit amet facilisis feugiat, odio metus
							gravida ante, ut pharetra massa metus id nunc. Duis scelerisque
							molestie turpis. Sed fringilla, massa eget luctus malesuada, metus
							eros molestie lectus, ut tempus eros massa ut dolor. Aenean
							aliquet fringilla sem. Suspendisse sed ligula in ligula suscipit
							aliquam. Praesent in eros vestibulum mi adipiscing adipiscing.
							Morbi facilisis. Curabitur ornare consequat nunc. Aenean vel
							metus. Ut posuere viverra nulla. Aliquam erat volutpat.
							Pellentesque convallis. Maecenas feugiat, tellus pellentesque
							pretium posuere, felis lorem euismod felis, eu ornare leo nisi vel
							felis. Mauris consectetur tortor et purus.</p>
					</div>
					<!--  <div id="tabs-3">
						<h2>Content heading 3</h2>
						<p>Mauris eleifend est et turpis. Duis id erat. Suspendisse
							potenti. Aliquam vulputate, pede vel vehicula accumsan, mi neque
							rutrum erat, eu congue orci lorem eget lorem. Vestibulum non ante.
							Class aptent taciti sociosqu ad litora torquent per conubia
							nostra, per inceptos himenaeos. Fusce sodales. Quisque eu urna vel
							enim commodo pellentesque. Praesent eu risus hendrerit ligula
							tempus pretium. Curabitur lorem enim, pretium nec, feugiat nec,
							luctus a, lacus.</p>
						<p>Duis cursus. Maecenas ligula eros, blandit nec, pharetra at,
							semper at, magna. Nullam ac lacus. Nulla facilisi. Praesent
							viverra justo vitae neque. Praesent blandit adipiscing velit.
							Suspendisse potenti. Donec mattis, pede vel pharetra blandit,
							magna ligula faucibus eros, id euismod lacus dolor eget odio. Nam
							scelerisque. Donec non libero sed nulla mattis commodo. Ut
							sagittis. Donec nisi lectus, feugiat porttitor, tempor ac, tempor
							vitae, pede. Aenean vehicula velit eu tellus interdum rutrum.
							Maecenas commodo. Pellentesque nec elit. Fusce in lacus. Vivamus a
							libero vitae lectus hendrerit hendrerit.</p>
					</div>-->
	
							</div> <!-- tabsvertical -->
						</div>
						</div>

					</td>
					<td valign="top" style="width: 97%;">
						<div id="tabs" style="width: 97%;">
							<!--  from https://jqueryui.com/tabs/ -->
							<ul>
								<li><a href="#infobox_text">Infobox text</a></li>
								<li><a href="#infobox_html">Original Wikipedia Infobox</a></li>

							</ul>
							<!--  <button id="showinfobox">Show Original Infobox</button>
										<button id="hideinfobox" style="display: none;">Hide
											Original Infobox</button>
												<br></br>
												
						<div id="infobox" style="display: none;">
							<div id="infobox_text"></div>
							
						</div>
						-->
							<div id="infobox_text"
								style="width: 95%; word-break: break-all; white-space: normal;"></div>
							<div id="infobox_html"
								style="width: 95%; word-break: break-all; white-space: normal; margin-left: auto; margin-right: auto;"></div>
						</div>
					</td>
				</tr>
			</table>





		</div>

	</div>
	<div id="footer">
		<p>
			The DBpedia SUE is an Open service hosted by the <a
				href="http://ai.wu.ac.at/">Institute for Information Business</a> at
			WU (<a href="http://wu.ac.at/">Vienna University of Economics and
				Business</a>).

		</p>
	</div>
</body>
</html>
