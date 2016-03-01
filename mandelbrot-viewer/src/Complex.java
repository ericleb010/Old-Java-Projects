/**
 * A class storing and manipulating the values of numbers on the complex plane. A number in the complex plane 
 * is expressed as the sum of some real number and the product of another real number with sqrt(-1), or a + bi.
 * @author Eric Leblanc
 * @version 1.0, 20/03/15
 */
public class Complex {
	private double real = 0;
	private double imag = 0;
	
	/**
	 * Constructor to build a complex number from scratch.
	 * @param inReal the "real" component of the number, a.
	 * @param inImag the "imaginary" component's multiplicand, b. 
	 */
	public Complex(double inReal, double inImag) {
		this.real = inReal;
		this.imag = inImag;
	}
	/**
	 * Constructor to build a complex number from another.
	 * @param c a Complex object, whose values will be copied.
	 */
	public Complex(Complex c) {
		this.real = c.getReal();
		this.imag = c.getImag();
	}
	
	/**
	 * Allows for the multiplication of two complex numbers, storing the result. The resulting complex number is described as 
	 * (a<sub>1</sub>a<sub>2</sub> - b<sub>1</sub>b<sub>2</sub>) + (a<sub>1</sub>b<sub>2</sub> + a<sub>2</sub>b<sub>1</sub>) <em>i</em> .
	 * @param toMult the other Complex object involved in the operation.
	 */
	public void multiply(Complex toMult) {
		// Product of two complex numbers is (a1a2 - b1b2) + (a1b2 + a2b1)i
		double newReal = ((this.real * toMult.getReal()) - (this.imag * toMult.getImag()));
		double newImag = ((this.real * toMult.getImag()) + (toMult.getReal() * this.imag));
		this.real = newReal;
		this.imag = newImag;
	}

	/**
	 * Allows for the addition of two complex numbers, storing the result. The resulting complex number is described as
	 * (a<sub>1</sub> + a<sub>2</sub>) + (b<sub>1</sub> + b<sub>2</sub>) <em>i</em> .
	 * @param toAdd the other Complex object involved in the operation.
	 */
	public void add(Complex toAdd) {
		// Sum of two complex numbers is (a1 + a2) + (b1 + b2)i
		this.real = (this.real + toAdd.getReal());
		this.imag = (this.imag + toAdd.getImag());
	}
	
	/**
	 * Allows for the subtraction of a complex number from this one, storing the result. The resulting complex number is described as 
	 * (a<sub>1</sub> - a<sub>2</sub>) + (b<sub>1</sub> - b<sub>2</sub>) <em>i</em> .
	 * @param toSub the other Complex object involved in the operation.
	 */
	public void subtract(Complex toSub) {
		// Subtraction of two complex numbers is (a1 - a2) + (b1 - b2)i
		this.real = (this.real - toSub.getReal());
		this.imag = (this.imag - toSub.getImag());
	}
	
	/**
	 * Returns the "real" component of this complex number.
	 * @return a double value, the "real" component.
	 */
	public double getReal() {
		return this.real;
	}
	
	/**
	 * Returns the "imaginary" component's multiplicand.
	 * @return a double value, the "imaginary" component.
	 */
	public double getImag() {
		return this.imag;
	}
	
	/**
	 * Returns the distance of this complex number's position from the origin of the plane.
	 * @return a double value, the distance from the origin.
	 */
	public double modulus() {
		// Modulus is sqrt(a^2 + b^2).
		return Math.sqrt(this.real * this.real + this.imag * this.imag);
	}
}
