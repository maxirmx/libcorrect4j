public class Field {
	byte[] exp_U;
	byte[] log;

	public byte fieldMulLogElement_U(byte l_U, byte r_U) {
		// like field_mul_log, but returns a field_element_t
		// because we are doing lookup here, we can safely skip the wrapover check
		short res_U = (short)(Short.toUnsignedInt((short)Byte.toUnsignedInt(l_U)) + Short.toUnsignedInt((short)Byte.toUnsignedInt(r_U)));
		return exp_U[Short.toUnsignedInt(res_U)];
	}

  public Field(short primitivePoly_U) {
    // in GF(2^8)
    // log and exp
    // bits are in GF(2), compute alpha^val in GF(2^8)
    // exp should be of size 512 so that it can hold a "wraparound" which prevents some modulo ops
    // log should be of size 256. no wraparound here, the indices into this table are field elements
  	exp_U = new byte[512];
	 	log_U = new byte[256];
  
    // assume alpha is a primitive element, p(x) (primitive_poly) irreducible in GF(2^8)
    // addition is xor
    // subtraction is addition (also xor)
    // e.g. x^5 + x^4 + x^4 + x^2 + 1 = x^5 + x^2 + 1
    // each row of exp contains the field element found by exponentiating
    //   alpha by the row index
    // each row of log contains the coefficients of
    //   alpha^7 + alpha^6 + alpha^5 + alpha^4 + alpha^3 + alpha^2 + alpha + 1
    // as 8 bits packed into one byte
      
    for(short i_U = 1; Short.toUnsignedInt(i_U) < 512; i_U++) {
			  element_U = (short)(Short.toUnsignedInt(element_U) * 2);
			  element_U = (short)(Short.toUnsignedInt(element_U) > 255 ? Short.toUnsignedInt(element_U) ^ Short.toUnsignedInt(primitivePoly_U) : Short.toUnsignedInt(element_U));
			  exp_U[Short.toUnsignedInt(i_U)] = (byte)element_U;
			  if(Short.toUnsignedInt(i_U) < 256) {
				  log_U.[Short.toUnsignedInt(element_U)] =  (byte)i_U;
    }
  }
    
	private static byte fieldAdd_U(byte l_U, byte r_U) {
		return (byte)(Byte.toUnsignedInt(l_U) ^ Byte.toUnsignedInt(r_U));
	}

	private static byte fieldSub_U(byte l_U, byte r_U) {
		return (byte)(Byte.toUnsignedInt(l_U) ^ Byte.toUnsignedInt(r_U));
	}

	private static byte fieldSum_U(byte elem_U, int n_U) {
		// we'll do a closed-form expression of the sum, although we could also
		//   choose to call field_add n times

		// since the sum is actually the bytewise XOR operator, this suggests two
		// kinds of values: n odd, and n even

		// if you sum once, you have coeff
		// if you sum twice, you have coeff XOR coeff = 0
		// if you sum thrice, you are back at coeff
		// an even number of XORs puts you at 0
		// an odd number of XORs puts you back at your value

		// so, just throw away all the even n
		return (byte)(Integer.remainderUnsigned(n_U, 2) != 0 ? Byte.toUnsignedInt(elem_U) : 0);
	}    
    
 	private byte fieldMul_U(byte l_U, byte r_U) {
		if(Byte.toUnsignedInt(l_U) == 0 || Byte.toUnsignedInt(r_U) == 0) {
			return 0;
		}
		// multiply two field elements by adding their logarithms.
		// yep, get your slide rules out
		short res_U = (short)(Short.toUnsignedInt((short)Byte.toUnsignedInt(log_U[Byte.toUnsignedInt(l_U)]) + 
                                               Short.toUnsignedInt((short)Byte.toUnsignedInt(field.log_U[Byte.toUnsignedInt(r_U)]));

		// if coeff exceeds 255, we would normally have to wrap it back around
		// alpha^255 = 1; alpha^256 = alpha^255 * alpha^1 = alpha^1
		// however, we've constructed exponentiation table so that
		//   we can just directly lookup this result
		// the result must be clamped to [0, 511]
		// the greatest we can see at this step is alpha^255 * alpha^255
		//   = alpha^510
		return exp_U[Short.toUnsignedInt(res_U)];
	}   
                                              
  private static byte fieldDiv_U(byte l_U, byte r_U) {
		if(Byte.toUnsignedInt(l_U) == 0) {
			return 0;
		}

		if(Byte.toUnsignedInt(r_U) == 0) {
			// XXX ???
			return 0;
		}

		// division as subtraction of logarithms

		// if rcoeff is larger, then log[l] - log[r] wraps under
		// so, instead, always add 255. in some cases, we'll wrap over, but
		// that's ok because the exp table runs up to 511.
		short res_U = (short)(Short.toUnsignedInt((short)255) + 
                          Short.toUnsignedInt((short)Byte.toUnsignedInt(log_U[Byte.toUnsignedInt(l_U)]) - 
                                                     Short.toUnsignedInt((short)Byte.toUnsignedInt(log_U[Byte.toUnsignedInt(r_U)]));
		return exp_U[Short.toUnsignedInt(res_U)];
	}

 	private static byte fieldMulLog_U(byte l_U, byte r_U) {
		// this function performs the equivalent of field_mul on two logarithms
		// we save a little time by skipping the lookup step at the beginning
		short res_U = (short)(Short.toUnsignedInt((short)Byte.toUnsignedInt(l_U)) + Short.toUnsignedInt((short)Byte.toUnsignedInt(r_U)));

		// because we arent using the table, the value we return must be a valid logarithm
		// which we have decided must live in [0, 255] (they are 8-bit values)
		// ensuring this makes it so that multiple muls will not reach past the end of the
		// exp table whenever we finally convert back to an element
		if(Short.toUnsignedInt(res_U) > 255) {
			return (byte)(Short.toUnsignedInt(res_U) - 255);
		}
		return (byte)res_U;
	}
                                              
	private static byte fieldDivLog_U(byte l_U, byte r_U) {
		// like field_mul_log, this performs field_div without going through a field_element_t
		short res_U = (short)(Short.toUnsignedInt((short)255) + Short.toUnsignedInt((short)Byte.toUnsignedInt(l_U)) - Short.toUnsignedInt((short)Byte.toUnsignedInt(r_U)));
		if(Short.toUnsignedInt(res_U) > 255) {
			return (byte)(Short.toUnsignedInt(res_U) - 255);
		}
		return (byte)res_U;
	}

	private static byte fieldPow_U(byte elem_U, int pow) {
		// take the logarithm, multiply, and then "exponentiate"
		// n.b. the exp table only considers powers of alpha, the primitive element
		// but here we have an arbitrary coeff
		byte log_U = log_U[Byte.toUnsignedInt(elem_U)];
		int resLog = Byte.toUnsignedInt(log_U) * pow;
		int mod = resLog % 255;
		if(mod < 0) {
			mod += 255;
		}
		return exp_U[mod];
	}
                                              
	}