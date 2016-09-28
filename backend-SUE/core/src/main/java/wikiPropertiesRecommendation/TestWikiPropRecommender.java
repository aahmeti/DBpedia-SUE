package wikiPropertiesRecommendation;

import java.util.HashSet;
import java.util.Set;

public class TestWikiPropRecommender {

	public static void main(String[] args) {

		WikiTemplateManager wikiRecommender = new WikiTemplateManager();

		String infoboxSearch = "Template:Infobox settlement";

		// test recommend alternative in Infobox settlement
		System.out.println("test recommend alternative in Template: Infobox settlement");
		Set<String> testAlternatives = new HashSet<String>();
		testAlternatives.add("area_total_km2");
		testAlternatives.add("area_total_sq_mi");
		testAlternatives.add("TotalArea_sq_mi");
		testAlternatives.add("area_total");

		String propertyRecommended = wikiRecommender.recommendTopAlternative(infoboxSearch, testAlternatives);
		System.out.println("Recommendation: " + propertyRecommended);

	}	
}
