/*
 * libcorrect4j
 * Polynomial.java
 * Created from src/correct/reed-solomon/polynomial.c @ https://github.com/quiet/libcorrect
 */

package libcorrect.reed_solomon;

import java.util.Arrays;

public class Polynomial implements Cloneable {
    private byte[] coeff_U;
    private int order_U;

    public Polynomial(int order) {
        coeff_U = new byte[order + 1];
        order_U = order;
    }

    public Polynomial(int order, byte[] coeff) {
        order_U = order;
        coeff_U = Arrays.copyOf(coeff, order_U+1);
    }


    public Polynomial(Polynomial p) {
        order_U = p.order_U;
        coeff_U = Arrays.copyOf(p.coeff_U, order_U+1);
    }

    public Polynomial clone() {
        return new Polynomial(this);
    }

    public int getOrder() {
        return order_U;
    }

    public void setOrder(int v) {
        order_U = v;
    }

    public byte getCoeff(int i) {
        return coeff_U[i];
    }

    public void setCoeff(int i, byte v) {
        coeff_U[i] = v;
    }

    public void flushCoeff() {
        Arrays.fill(coeff_U, (byte)0);
    }


    // if you want a full multiplication, then make res.order = l.order + r.order
    // but if you just care about a lower order, e.g. mul mod x^i, then you can select
    //    fewer coefficients

    public static void polynomialMul(Field field, Polynomial l, Polynomial r, Polynomial res) {
        // perform an element-wise multiplication of two polynomials
        Arrays.fill(res.coeff_U, (byte) 0);
        for (int i_U = 0; Integer.compareUnsigned(i_U, l.order_U) <= 0; i_U++) {
            if (Integer.compareUnsigned(i_U, res.order_U) > 0) {
                continue;
            }
            int jLimit_U = Integer.compareUnsigned(r.order_U, res.order_U - i_U) > 0 ? res.order_U - i_U : r.order_U;
            for (int j_U = 0; Integer.compareUnsigned(j_U, jLimit_U) <= 0; j_U++) {
                // e.g. alpha^5*x * alpha^37*x^2 --> alpha^42*x^3
                res.coeff_U[i_U + j_U] = field.fieldAdd(res.coeff_U[i_U + j_U],
                        field.fieldMul(l.coeff_U[i_U], r.coeff_U[j_U]));
            }
        }
    }

    public static void polynomialMod(Field field, Polynomial dividend, Polynomial divisor, Polynomial mod) {
        // find the polynomial remainder of dividend mod divisor
        // do long division and return just the remainder (written to mod)

        if (Integer.compareUnsigned(mod.order_U, dividend.order_U) < 0) {
            // mod.order must be >= dividend.order (scratch space needed)
            // this is an error -- catch it in debug?
            return;
        }
        // initialize remainder as dividend
        mod.coeff_U = Arrays.copyOf(dividend.coeff_U, dividend.order_U + 1);


        // XXX make sure divisor[divisor_order] is nonzero
        byte divisorLeading_U = field.log_U[Byte.toUnsignedInt(divisor.coeff_U[divisor.order_U])];

        // long division steps along one order at a time, starting at the highest order
        for (int i_U = dividend.order_U; Integer.compareUnsigned(i_U, 0) > 0; i_U--) {
            // look at the leading coefficient of dividend and divisor
            // if leading coefficient of dividend / leading coefficient of divisor is q
            //   then the next row of subtraction will be q * divisor
            // if order of q < 0 then what we have is the remainder and we are done
            if (Integer.compareUnsigned(i_U, divisor.order_U) < 0) {
                break;
            }
            if (Byte.toUnsignedInt(mod.coeff_U[i_U]) == 0) {
                continue;
            }
            int qOrder_U = i_U - divisor.order_U;
            byte qCoeff_U = (byte) field.fieldDivLog(field.log_U[Byte.toUnsignedInt(mod.coeff_U[i_U])], divisorLeading_U);

            // now that we've chosen q, multiply the divisor by q and subtract from
            //   our remainder. subtracting in GF(2^8) is XOR, just like addition
            for (int j_U = 0; Integer.compareUnsigned(j_U, divisor.order_U) <= 0; j_U++) {
                if (Byte.toUnsignedInt(divisor.coeff_U[j_U]) == 0) {
                    continue;
                }
                // all of the multiplication is shifted up by q_order places
                mod.coeff_U[j_U + qOrder_U] = field.fieldAdd(mod.coeff_U[j_U + qOrder_U],
                        field.fieldMulLogElement(field.log_U[Byte.toUnsignedInt(divisor.coeff_U[j_U])], qCoeff_U));

            }
        }
    }

    public static void polynomialFormalDerivative(Field field, Polynomial poly, Polynomial der) {
        // if f(x) = a(n)*x^n + ... + a(1)*x + a(0)
        // then f'(x) = n*a(n)*x^(n-1) + ... + 2*a(2)*x + a(1)
        // where n*a(n) = sum(k=1, n, a(n)) e.g. the nth sum of a(n) in GF(2^8)

        // assumes der.order = poly.order - 1
        Arrays.fill(der.coeff_U, (byte) 0);
        for (int i_U = 0; Integer.compareUnsigned(i_U, der.order_U) <= 0; i_U++) {
            // we're filling in the ith power of der, so we look ahead one power in poly
            // f(x) = a(i + 1)*x^(i + 1) -> f'(x) = (i + 1)*a(i + 1)*x^i
            // where (i + 1)*a(i + 1) is the sum of a(i + 1) (i + 1) times, not the product
            der.coeff_U[i_U] = field.fieldSum(poly.coeff_U[i_U + 1], i_U + 1);
        }
    }

    public static byte polynomialEval(Field field, Polynomial poly, byte val_U) {
        // evaluate the polynomial poly at a particular element val
        if (Byte.toUnsignedInt(val_U) == 0) {
            return poly.coeff_U[0];
        }

        byte res_U = 0;

        // we're going to start at 0th order and multiply by val each time
        byte valExponentiated_U = field.log_U[1];
        byte valLog_U = field.log_U[Byte.toUnsignedInt(val_U)];

        for (int i_U = 0; Integer.compareUnsigned(i_U, poly.order_U) <= 0; i_U++) {
            if (Byte.toUnsignedInt(poly.coeff_U[i_U]) != 0) {
                // multiply-accumulate by the next coeff times the next power of val
                res_U = (byte) field.fieldAdd(res_U, field.fieldMulLogElement(field.log_U[Byte.toUnsignedInt(poly.coeff_U[i_U])],
                                                                              valExponentiated_U));
            }
            // now advance to the next power
            valExponentiated_U = (byte) field.fieldMulLog(valExponentiated_U, valLog_U);
        }
        return res_U;
    }

    public byte polynomialEvalLut(Field field, byte[] valExp_U) {
        // evaluate the polynomial poly at a particular element val
        // in this case, all of the logarithms of the successive powers of val have been precalculated
        // this removes the extra work we'd have to do to calculate val_exponentiated each time
        //   if this function is to be called on the same val multiple times
        if (Byte.toUnsignedInt(valExp_U[0]) == 0) {
            return coeff_U[0];
        }

        byte res_U = 0;

        for (int i_U = 0; Integer.compareUnsigned(i_U, order_U) <= 0; i_U++) {
            if (Byte.toUnsignedInt(coeff_U[i_U]) != 0) {
                // multiply-accumulate by the next coeff times the next power of val
                res_U = field.fieldAdd(res_U, field.fieldMulLogElement(field.log_U[Byte.toUnsignedInt(coeff_U[i_U])], valExp_U[i_U]));
            }
        }
        return res_U;
    }

    public byte polynomialEvalLogLut(Field field, byte[] valExp_U) {
        // evaluate the log_polynomial poly at a particular element val
        // like polynomial_eval_lut, the logarithms of the successive powers of val have been
        //   precomputed
        if (Byte.toUnsignedInt(valExp_U[0]) == 0) {
            if (Byte.toUnsignedInt(coeff_U[0]) == 0) {
                // special case for the non-existant log case
                return 0;
            }
            return field.exp_U[Byte.toUnsignedInt(coeff_U[0])];
        }

        byte res_U = 0;

        for (int i_U = 0; Integer.compareUnsigned(i_U, order_U) <= 0; i_U++) {
            // using 0 as a sentinel value in log -- log(0) is really -inf
            if (Byte.toUnsignedInt(coeff_U[i_U]) != 0) {
                // multiply-accumulate by the next coeff times the next power of val
                res_U = (byte) field.fieldAdd(res_U, field.fieldMulLogElement(coeff_U[i_U], valExp_U[i_U]));
            }
        }
        return res_U;
    }

    public static void polynomialBuildExpLut(Field field, byte val_U, int order_U, byte[] valExp_U) {
        // create the lookup table of successive powers of val used by polynomial_eval_lut
        byte valExponentiated_U = field.log_U[1];
        byte valLog_U = field.log_U[Byte.toUnsignedInt(val_U)];
        for (int i_U = 0; Integer.compareUnsigned(i_U, order_U) <= 0; i_U++) {
            if (Byte.toUnsignedInt(val_U) == 0) {
                valExp_U[i_U] = (byte) 0;
            } else {
                valExp_U[i_U] = valExponentiated_U;
                valExponentiated_U = field.fieldMulLog(valExponentiated_U, valLog_U);
            }
        }
    }

    public static Polynomial polynomialInitFromRoots(Field field,
                                                     int nroots_U,
                                                     byte[] roots_U,
                                                     Polynomial  poly,
                                                     Polynomial[] scratch) {
        int order_U = nroots_U;
        Polynomial l = new Polynomial(1);

        // we'll keep two temporary stores of rightside polynomial
        // each time through the loop, we take the previous result and use it as new rightside
        // swap back and forth (prevents the need for a copy)
        Polynomial[] r = new Polynomial[2];
        r[0] = scratch[0].clone();
        r[1] = scratch[1].clone();
        int rcoeffres_U = 0;

        // initialize the result with x + roots[0]
        r[rcoeffres_U].coeff_U[1] = (byte) 1;
        r[rcoeffres_U].coeff_U[0] = roots_U[0];
        r[rcoeffres_U].order_U = 1;

        // initialize lcoeff[1] with x
        // we'll fill in the 0th order term in each loop iter
        l.coeff_U[1] = (byte) 1;

        // loop through, using previous run's result as the new right hand side
        // this allows us to multiply one group at a time
        for (int i_U = 1; Integer.compareUnsigned(i_U, nroots_U) < 0; i_U++) {
            l.coeff_U[0] = roots_U[i_U];
            int nextrcoeff_U = rcoeffres_U;
            rcoeffres_U = Integer.remainderUnsigned(rcoeffres_U + 1, 2);
            r[rcoeffres_U].order_U = i_U + 1;
            polynomialMul(field, l, r[nextrcoeff_U], r[rcoeffres_U]);
        }

        poly.coeff_U = Arrays.copyOf(r[rcoeffres_U].coeff_U, order_U + 1);
        poly.order_U = order_U;

        return poly;
    }

    public static Polynomial polynomialCreateFromRoots(Field field, int nroots_U, byte[] roots_U) {
        Polynomial poly = new Polynomial(nroots_U);
        int order_U = nroots_U;
        Polynomial l = new Polynomial(1);

        Polynomial[] r = new Polynomial[2];
        // we'll keep two temporary stores of rightside polynomial
        // each time through the loop, we take the previous result and use it as new rightside
        // swap back and forth (prevents the need for a copy)

        r[0] = new Polynomial(1);
        r[1] = new Polynomial(order_U);
        int rcoeffres_U = 0;

        // initialize the result with x + roots[0]
        r[rcoeffres_U].coeff_U[0] = roots_U[0];
        r[rcoeffres_U].coeff_U[1] = (byte) 1;

        // initialize lcoeff[1] with x
        // we'll fill in the 0th order term in each loop iter
        l.coeff_U[1] = (byte) 1;

        // loop through, using previous run's result as the new right hand side
        // this allows us to multiply one group at a time
        for (int i_U = 1; Integer.compareUnsigned(i_U, nroots_U) < 0; i_U++) {
            l.coeff_U[0] =  roots_U[i_U];
            int nextrcoeff_U = rcoeffres_U;
            rcoeffres_U = Integer.remainderUnsigned(rcoeffres_U + 1, 2);
            r[rcoeffres_U].order_U = i_U + 1;
            polynomialMul(field, l, r[nextrcoeff_U], r[rcoeffres_U]);
        }

        poly.coeff_U = Arrays.copyOf(r[rcoeffres_U].coeff_U, order_U + 1);
        poly.order_U = order_U;

        return poly;
    }
}

