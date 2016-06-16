package hdtppJNI;

public class Testlauncher {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HDTManager loader = new HDTManager();
		//HDTpp hdt = loader.cargarHDT("ECyL.hdtpp");
		HDTpp hdt = loader.cargarHDT("dbpedia2015.hdtpp");

		String predicates = "http://dbpedia.org/ontology/wikiPageID|http://dbpedia.org/ontology/wikiPageRevisionID|http://www.w3.org/2000/01/rdf-schema#label|http://www.w3.org/2002/07/owl#sameAs|http://www.w3.org/2004/02/skos/core#prefLabel|http://www.w3.org/ns/prov#wasDerivedFrom";

		//String subjects= hdt.findSimilarSubjectswithPredicates("http://purl.org/dc/terms/isPartOf|http://xmlns.com/foaf/0.1/name");
		String subjects= hdt.findSimilarSubjectswithPredicates(predicates);
		//System.out.println("subjects:"+subjects);
		String[] subjs = subjects.split("\\|");
		for (int i =0;i<subjs.length;i++){
			System.out.println("Subject: " + subjs[i]);	
		}

	}

}
