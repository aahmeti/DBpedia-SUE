package com.wu.dbpediaupdate;

import java.util.ArrayList;

public class WikiUpdateResponse {

	protected ArrayList<WikiUpdate> wikiUpdates; // to date, one per Triple
	// Pattern

	protected String query;
	
	public WikiUpdateResponse() {
		wikiUpdates = new ArrayList<WikiUpdate>();
	}


	public ArrayList<WikiUpdate> getWikiUpdates() {
		return wikiUpdates;
	}

	public void setWikiUpdates(ArrayList<WikiUpdate> wikiUpdates) {
		this.wikiUpdates = wikiUpdates;
	}

	public void addWikiUpdate(WikiUpdate wikiUpd) {
		wikiUpdates.add(wikiUpd);
	}
	
	public String getQuery() {
		return query;
	}


	public void setQuery(String query) {
		this.query = query;
	}

	/*
	 * @Override public String toString() { return "WikiUpdateResponse []"; }
	 */

	public class WikiUpdate {

		protected ArrayList<WikiAccomodation> wikiAccommodations; // alternative
																	// accomodations
		protected String wikiTitle;

		protected String originalInfobox;

		public WikiUpdate() {
			wikiAccommodations = new ArrayList<WikiAccomodation>();
		}

		public String getOriginalInfobox() {
			return originalInfobox;
		}

		public void setOriginalInfobox(String originalInfobox) {
			this.originalInfobox = originalInfobox;
		}

		public String getWikiTitle() {
			return wikiTitle;
		}

		public void setWikiTitle(String wikiTitle) {
			this.wikiTitle = wikiTitle;
		}

		public void addWikiAccommodation(WikiAccomodation wikiAcc) {
			wikiAccommodations.add(wikiAcc);
		}

		public ArrayList<WikiAccomodation> getWikiAccommodations() {
			return wikiAccommodations;
		}

		public void setWikiAccommodations(
				ArrayList<WikiAccomodation> wikiAccommodations) {
			this.wikiAccommodations = wikiAccommodations;
		}

		public class WikiAccomodation {

			public WikiAccomodation() {
			}

			public WikiAccomodation(String addsWikipedia, String delsWikipedia,
					String addsDBpedia, String delsDBpedia) {
				this.addsWikipedia = addsWikipedia;
				this.delsWikipedia = delsWikipedia;
				this.addsDBpedia = addsDBpedia;
				this.delsDBpedia = delsDBpedia;
			}

			protected String addsWikipedia;
			protected String delsWikipedia;
			protected String addsDBpedia;
			protected String delsDBpedia;

			public String getAddsWikipedia() {
				return addsWikipedia;
			}

			public void setAddsWikipedia(String addsWikipedia) {
				this.addsWikipedia = addsWikipedia;
			}

			public String getDelsWikipedia() {
				return delsWikipedia;
			}

			public void setDelsWikipedia(String delsWikipedia) {
				this.delsWikipedia = delsWikipedia;
			}

			public String getAddsDBpedia() {
				return addsDBpedia;
			}

			public void setAddsDBpedia(String addsDBpedia) {
				this.addsDBpedia = addsDBpedia;
			}

			public String getDelsDBpedia() {
				return delsDBpedia;
			}

			public void setDelsDBpedia(String delsDBpedia) {
				this.delsDBpedia = delsDBpedia;
			}
		}

	}

	
}
