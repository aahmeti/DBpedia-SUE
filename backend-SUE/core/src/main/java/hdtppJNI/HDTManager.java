package hdtppJNI;

public class HDTManager {

	static {
		System.loadLibrary("hdt-jni");
	}
	private long handler;

	private native long cargarHDTNativo(String file);

	public HDTManager() {
	}// Constructors sin parametrizar

	public HDTpp cargarHDT(String file) {
		long handle = cargarHDTNativo(file);
		return new HDTpp(handle);
	}
}
