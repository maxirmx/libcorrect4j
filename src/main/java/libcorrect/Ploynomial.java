public class Polynomial {
	private byte[] coeff_U;
        private int order_U;
  
  	public Polynomial(int order_U) {
		coeff_U = new byte[order_U + 1];
		this.order_U = order_U;
	}

	// if you want a full multiplication, then make res.order = l.order + r.order
	// but if you just care about a lower order, e.g. mul mod x^i, then you can select
	//    fewer coefficients
	
	public static void polynomialMul(Field field, Polynomial l, PolynomialT r, Polynomial res) {
		// perform an element-wise multiplication of two polynomials
		Polynomial res = new Polynomial() 
		res.coeff_U.fill(0, res.order_U + 1, (byte)0);
		for(int i_U = 0; Integer.compareUnsigned(i_U, l.order_U) <= 0; i_U++) {
			if(Integer.compareUnsigned(i_U, res.order_U) > 0) {
				continue;
			}
			int jLimit_U = Integer.compareUnsigned(r.order_U, res.order_U - i_U) > 0 ? res.order_U - i_U : r.order_U;
			for(int j_U = 0; Integer.compareUnsigned(j_U, jLimit_U) <= 0; j_U++) {
				// e.g. alpha^5*x * alpha^37*x^2 --> alpha^42*x^3
				res.getCoeff_U().set(i_U + j_U, (byte)fieldAdd(field, 
									       res.coeff_U[i_U + j_U], 
									       fieldMul(field, l.coeff_U[i_U], r.coeff_U[j_U]));
			}
		}
	}
						     
	public static void polynomialMod(FieldT field, PolynomialT dividend, PolynomialT divisor, PolynomialT mod) {
		// find the polynomial remainder of dividend mod divisor
		// do long division and return just the remainder (written to mod)

		if(Integer.compareUnsigned(mod.order_U(), dividend.order_U()) < 0) {
			// mod.order must be >= dividend.order (scratch space needed)
			// this is an error -- catch it in debug?
			return;
		}
		// initialize remainder as dividend
		nnc(mod.getCoeff_U()).copyFrom(dividend.getCoeff_U(), dividend.getOrder_U() + 1);
		
		// long division steps along one order at a time, starting at the highest order
		for(int i_U = dividend.getOrder_U(); Integer.compareUnsigned(i_U, 0) > 0; i_U--) {
					// look at the leading coefficient of dividend and divisor
			// if leading coefficient of dividend / leading coefficient of divisor is q
			//   then the next row of subtraction will be q * divisor
			// if order of q < 0 then what we have is the remainder and we are done
			if(Integer.compareUnsigned(i_U, divisor.getOrder_U()) < 0) {
				break;
			}
			if(Byte.toUnsignedInt(mod.getCoeff_U().get(i_U)) == 0) {
				continue;
			}
			int qOrder_U = i_U - divisor.getOrder_U();
			byte qCoeff_U = (byte)fieldDivLog(field.copy(), field.getLog_U().get(Byte.toUnsignedInt(mod.getCoeff_U().get(i_U))), divisorLeading_U);

			// now that we've chosen q, multiply the divisor by q and subtract from
			//   our remainder. subtracting in GF(2^8) is XOR, just like addition
			for(int j_U = 0; Integer.compareUnsigned(j_U, divisor.getOrder_U()) <= 0; j_U++) {
				if(Byte.toUnsignedInt(divisor.getCoeff_U().get(j_U)) == 0) {
					continue;
				}
								// all of the multiplication is shifted up by q_order places
				mod.getCoeff_U().set(j_U + qOrder_U, (byte)fieldAdd(field.copy(), mod.getCoeff_U().get(j_U + qOrder_U), fieldMulLogElement(field.copy(), field.getLog_U().get(Byte.toUnsignedInt(divisor.getCoeff_U().get(j_U))), qCoeff_U)));

			}
	}
		
	public static void polynomialFormalDerivative(FieldT field, PolynomialT poly, PolynomialT der) {
		// if f(x) = a(n)*x^n + ... + a(1)*x + a(0)
		// then f'(x) = n*a(n)*x^(n-1) + ... + 2*a(2)*x + a(1)
		// where n*a(n) = sum(k=1, n, a(n)) e.g. the nth sum of a(n) in GF(2^8)

		// assumes der.order = poly.order - 1
		der.getCoeff_U().fill(0, der.getOrder_U() + 1, (byte)0);
		for(int i_U = 0; Integer.compareUnsigned(i_U, der.getOrder_U()) <= 0; i_U++) {
			// we're filling in the ith power of der, so we look ahead one power in poly
			// f(x) = a(i + 1)*x^(i + 1) -> f'(x) = (i + 1)*a(i + 1)*x^i
			// where (i + 1)*a(i + 1) is the sum of a(i + 1) (i + 1) times, not the product
			der.getCoeff_U().set(i_U, (byte)fieldSum(field.copy(), poly.getCoeff_U().get(i_U + 1), i_U + 1));
		}
	}
			public static byte polynomialEval_U(FieldT field, PolynomialT poly, byte val_U) {
		// evaluate the polynomial poly at a particular element val
		if(Byte.toUnsignedInt(val_U) == 0) {
			return poly.getCoeff_U().get(0);
		}

		byte res_U = 0;

		// we're going to start at 0th order and multiply by val each time
		byte valExponentiated_U = field.getLog_U().get(1);
		byte valLog_U = field.getLog_U().get(Byte.toUnsignedInt(val_U));
		
		for(int i_U = 0; Integer.compareUnsigned(i_U, poly.getOrder_U()) <= 0; i_U++) {
			if(Byte.toUnsignedInt(poly.getCoeff_U().get(i_U)) != 0) {
				// multiply-accumulate by the next coeff times the next power of val
				res_U = (byte)fieldAdd(field.copy(), res_U, fieldMulLogElement(field.copy(), field.getLog_U().get(Byte.toUnsignedInt(poly.getCoeff_U().get(i_U))), valExponentiated_U));
			}
			// now advance to the next power
			valExponentiated_U = (byte)fieldMulLog(field.copy(), valExponentiated_U, valLog_U);
		}
		return res_U;
	}

	public static byte polynomialEvalLut_U(FieldT field, PolynomialT poly, byte[] valExp_U) {
		    // evaluate the polynomial poly at a particular element val
    // in this case, all of the logarithms of the successive powers of val have been precalculated
    // this removes the extra work we'd have to do to calculate val_exponentiated each time
    //   if this function is to be called on the same val multiple times
		if(Byte.toUnsignedInt(valExp_U[0]) == 0) {
			return poly.getCoeff_U().get(0);
		}

		byte res_U = 0;

		for(int i_U = 0; Integer.compareUnsigned(i_U, poly.getOrder_U()) <= 0; i_U++) {
			if(Byte.toUnsignedInt(poly.getCoeff_U().get(i_U)) != 0) {
				// multiply-accumulate by the next coeff times the next power of val
				res_U = (byte)fieldAdd(field.copy(), res_U, fieldMulLogElement(field.copy(), field.getLog_U().get(Byte.toUnsignedInt(poly.getCoeff_U().get(i_U))), valExp_U[i_U]));
			}
		}
		return res_U;
	}
		
			public static byte polynomialEvalLogLut_U(FieldT field, PolynomialT polyLog, byte[] valExp_U) {
		// evaluate the log_polynomial poly at a particular element val
		// like polynomial_eval_lut, the logarithms of the successive powers of val have been
		//   precomputed
		if(Byte.toUnsignedInt(valExp_U[0]) == 0) {
			if(Byte.toUnsignedInt(polyLog.getCoeff_U().get(0)) == 0) {
				// special case for the non-existant log case
				return 0;
			}
			return field.getExp_U().get(Byte.toUnsignedInt(polyLog.getCoeff_U().get(0)));
		}
			for(int i_U = 0; Integer.compareUnsigned(i_U, polyLog.getOrder_U()) <= 0; i_U++) {
			// using 0 as a sentinel value in log -- log(0) is really -inf
			if(Byte.toUnsignedInt(polyLog.getCoeff_U().get(i_U)) != 0) {
				// multiply-accumulate by the next coeff times the next power of val
				res_U = (byte)fieldAdd(field.copy(), res_U, fieldMulLogElement(field.copy(), polyLog.getCoeff_U().get(i_U), valExp_U[i_U]));
			}
		}
		return res_U;
	}
		public static void polynomialBuildExpLut(FieldT field, byte val_U, int order_U, byte[] valExp_U) {
		// create the lookup table of successive powers of val used by polynomial_eval_lut
		byte valExponentiated_U = field.getLog_U().get(1);
		byte valLog_U = field.getLog_U().get(Byte.toUnsignedInt(val_U));
		for(int i_U = 0; Integer.compareUnsigned(i_U, order_U) <= 0; i_U++) {
			if(Byte.toUnsignedInt(val_U) == 0) {
				valExp_U[i_U] = (byte)0;
			} else {
				valExp_U[i_U] = valExponentiated_U;
				valExponentiated_U = (byte)fieldMulLog(field.copy(), valExponentiated_U, valLog_U);
			}
		}
	}
		
		public static PolynomialT polynomialInitFromRoots(FieldT field, int nroots_U, byte[] roots_U, PolynomialT poly, PolynomialT[] scratch) {
		int order_U = nroots_U;
		PolynomialT l = new PolynomialT();
		String8 lCoeff_U = new String8(2);
		l.setOrder_U(1);
		l.setCoeff_U(lCoeff_U);

		// we'll keep two temporary stores of rightside polynomial
		// each time through the loop, we take the previous result and use it as new rightside
		// swap back and forth (prevents the need for a copy)
		PolynomialT[] r = Stream.generate(() -> new PolynomialT()).limit(2).toArray(PolynomialT[]::new);
		r[0] = scratch[0].copy();
		r[1] = scratch[1].copy();
		int rcoeffres_U = 0;

		// initialize the result with x + roots[0]
		r[rcoeffres_U].getCoeff_U().set(1, (byte)1);
		r[rcoeffres_U].getCoeff_U().set(0, roots_U[0]);
		r[rcoeffres_U].setOrder_U(1);
		
    // initialize lcoeff[1] with x
    // we'll fill in the 0th order term in each loop iter			
					l.getCoeff_U().set(1, (byte)1);

		// loop through, using previous run's result as the new right hand side
		// this allows us to multiply one group at a time
		for(int i_U = 1; Integer.compareUnsigned(i_U, nroots_U) < 0; i_U++) {
			l.getCoeff_U().set(0, roots_U[i_U]);
			int nextrcoeff_U = rcoeffres_U;
			rcoeffres_U = Integer.remainderUnsigned(rcoeffres_U + 1, 2);
			r[rcoeffres_U].setOrder_U(i_U + 1);
			polynomialMul(field.copy(), l.copy(), r[nextrcoeff_U].copy(), r[rcoeffres_U].copy());
		}

		nnc(poly.getCoeff_U()).copyFrom(r[rcoeffres_U].getCoeff_U(), order_U + 1);
		poly.setOrder_U(order_U);

		return poly;
	}
		
			public static PolynomialT polynomialCreateFromRoots(FieldT field, int nroots_U, byte[] roots_U) {
		PolynomialT poly = polynomialCreate(nroots_U);
		int order_U = nroots_U;
		PolynomialT l = new PolynomialT();
		l.setOrder_U(1);
		l.setCoeff_U(new String8(true, 2));

		PolynomialT[] r = Stream.generate(() -> new PolynomialT()).limit(2).toArray(PolynomialT[]::new);
		// we'll keep two temporary stores of rightside polynomial
		// each time through the loop, we take the previous result and use it as new rightside
		// swap back and forth (prevents the need for a copy)
		r[0].setCoeff_U(new String8(true, order_U + 1));
		r[1].setCoeff_U(new String8(true, order_U + 1));
		int rcoeffres_U = 0;

		// initialize the result with x + roots[0]
		r[rcoeffres_U].getCoeff_U().set(0, roots_U[0]);
		r[rcoeffres_U].getCoeff_U().set(1, (byte)1);
		r[rcoeffres_U].setOrder_U(1);
				
	    // initialize lcoeff[1] with x
    // we'll fill in the 0th order term in each loop iter	
						l.getCoeff_U().set(1, (byte)1);

		// loop through, using previous run's result as the new right hand side
		// this allows us to multiply one group at a time
		for(int i_U = 1; Integer.compareUnsigned(i_U, nroots_U) < 0; i_U++) {
			l.getCoeff_U().set(0, roots_U[i_U]);
			int nextrcoeff_U = rcoeffres_U;
			rcoeffres_U = Integer.remainderUnsigned(rcoeffres_U + 1, 2);
			r[rcoeffres_U].setOrder_U(i_U + 1);
			polynomialMul(field.copy(), l.copy(), r[nextrcoeff_U].copy(), r[rcoeffres_U].copy());
		}

		nnc(poly.getCoeff_U()).copyFrom(r[rcoeffres_U].getCoeff_U(), order_U + 1);
		poly.setOrder_U(order_U);

		nnc(l.getCoeff_U()).setNativeControlled(false);
		nnc(r[0].getCoeff_U()).setNativeControlled(false);
		nnc(r[1].getCoeff_U()).setNativeControlled(false);

		return poly;
	}
	}
    }
