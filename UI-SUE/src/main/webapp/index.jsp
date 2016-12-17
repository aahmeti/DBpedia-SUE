<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></meta>
<title>DBpedia-SUE -- The DBpedia SPARQL Update Endpoint</title>
 
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
  });
  </script>
<script id="js">
	$(document)
			.ready(
		
      
					function() {
						
						$('#formSearch')
								.submit(
										function() { // catch the form's submit event
										
										$( "#tabsverticalTriples" ).tabs();
										$( "#tabsverticalTriples" ).tabs( "destroy" );
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
														 },
														success : function(
																response) { // on success..
																
														 	$("#dialog").dialog('close');
															$("#results").show(1000);
														$('#infobox_text').html($($.parseHTML(response)).filter("#jsoninfoboxes"));
														$('#tabsmenuTriples').html($($.parseHTML(response)).filter("#tabsTriples"));
														
														
														// this works but prevent to reuse in several calls
														
														$('#tabscontentTriples').html($($.parseHTML(response)).filter("#alternativesSubjects"));
														  
														   $( "#tabsverticalTriples" ).tabs();
														  $( "#tabsverticalTriples li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
														
														var title = $($.parseHTML(response)).filter("#title_wiki").html();
														$('#infobox_html').load("http://crossorigin.me/https://en.wikipedia.org/wiki/"+title+" .infobox");
													     
													      $( "#progressbar" ).progressbar( "value", false );
													      $( ".progress-label" )
													        .text( "Starting update..." );
													        $('#js2').html(""); //test
															//$( "#tabsverticalTriples" ).append($($.parseHTML(response)).filter("#js2"));
															
															$( "#tabJS" ).html("");
															$( "#tabJS" ).append($($.parseHTML(response)).filter("#js2"));
															
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
        			 //alert(data);
        	
        		if (data.length>0){
		        	$("#resultConsistency").html("<img src=\"img/Loading_icon.gif\" height=\"75px;\">");
		        	
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
										
										 $("#resultConsistency").html("<span style=\"background-color:lightgreen;\">This update is consistent!</span>");
										
											
										},
										error : function(xhr,
												status) {
											if (status !='abort') {
												
													 $("#resultConsistency").html("<span style=\"background-color:red;\">This update is NOT consistent!</span>");
											}
											
											
										}
									});
						return false; // cancel original event to prevent form submitting					
				}
				else{
				$("#resultConsistency").html("<span style=\"background-color:orange;\">Please chek at least one update!</span>");
				}
				
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
		<form id="formSearch" method="post">
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
			<input type="text" style="display: none;" name="valueAction"
				id="valueAction" value="webapi/ajaxupdateSingletonRecode"></input>
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
				
				<br>
				
				<h4>- Choose Semantics <select
					name="format" id="format" onchange="format_change(this)">
					<option value="auto">Auto</option>
					<option value="brave" selected="selected">Brave</option>
					<option value="cautious">Cautious</option>
					<option value="fainthearted">Fainthearted</option>
				</select> 
				</h4>
					<h4 style="border:10px;">- <input type="checkbox" name="stats" value="true"/> Compute statistics of similar subjects (wait a few seconds more). Sample: <input type="number" name="sample" value="50" min="1" max="300" /></h4>
				
				
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
								
							</div> <!-- tabsvertical -->
						</div>
						</div>
						<div id="tabJS"></div> 

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
