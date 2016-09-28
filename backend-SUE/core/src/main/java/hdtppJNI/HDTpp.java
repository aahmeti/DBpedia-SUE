package hdtppJNI;

public class HDTpp {
	private long handler;

	// 
	public native String findSimilarSubjectswithPredicatesNativo(String predicates);

	//To go back to the same memory address where the file was loaded
	public native void reiniciarHDT();

	public HDTpp(long handle) {
		handler = handle;// Get the handle of the loaded file
	}

	public String findSimilarSubjectswithPredicates(String predicates) {
		return findSimilarSubjectswithPredicatesNativo(predicates);
	}
}