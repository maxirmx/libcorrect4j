public class PolynomialT extends NativeClass {
	private byte[] coeff_U;
  private int order_U;
  
  	public Polynomial(int order_U) {
		coeff_U = new byte[order_U + 1];
		this.order_U = order_U;
	}
