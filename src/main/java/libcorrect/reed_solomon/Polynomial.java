/*
 * libcorrect4j
 * Polynomial.java
 * Created from src/correct/reed-solomon/polynomial.c @ https://github.com/quiet/libcorrect
 */

package libcorrect.reed_solomon;

import java.util.Arrays;

public class Polynomial implements Cloneable {
    private byte[] coeff;
    private int order;

    public Polynomial(int order) {
        coeff = new byte[order + 1];
        this.order = order;
    }

    public Polynomial(int order, byte[] coeff) {
        this.order = order;
        this.coeff = Arrays.copyOf(coeff, this.order +1);
    }

    public void copyCoeff(Polynomial p) {
        for (int i = 0; i<p.coeff.length; i++) {
            coeff[i] = p.coeff[i];
        }
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int v) {
        order = v;
    }

    public byte[] getCoeff() {
        return coeff;
    }

    public byte getCoeff(int i) {
        return coeff[i];
    }

    public void setCoeff(int i, byte v) {
        coeff[i] = v;
    }

    public void flushCoeff() {
        Arrays.fill(coeff, (byte)0);
    }


    // if you want a full multiplication, then make res.order = l.order + r.order
    // but if you just care about a lower order, e.g. mul mod x^i, then you can select
    //    fewer coefficients

    public static void polynomialMul(Field field, Polynomial l, Polynomial r, Polynomial res) {
        // perform an element-wise multiplication of two polynomials
        Arrays.fill(res.coeff, (byte) 0);
        for (int i = 0; Integer.compareUnsigned(i, l.order) <= 0; i++) {
            if (Integer.compareUnsigned(i, res.order) > 0) {
                continue;
            }
            int jLimit_U = Integer.compareUnsigned(r.order, res.order - i) > 0 ? res.order - i : r.order;
            for (int j = 0; Integer.compareUnsigned(j, jLimit_U) <= 0; j++) {
                // e.g. alpha^5*x * alpha^37*x^2 --> alpha^42*x^3
                res.coeff[i + j] = field.fieldAdd(res.coeff[i + j],
                        field.fieldMul(l.coeff[i], r.coeff[j]));
            }
        }
    }

    public static void polynomialMod(Field field, Polynomial dividend, Polynomial divisor, Polynomial mod) {
        // find the polynomial remainder of dividend mod divisor
        // do long division and return just the remainder (written to mod)

        if (Integer.compareUnsigned(mod.order, dividend.order) < 0) {
            // mod.order must be >= dividend.order (scratch space needed)
            // this is an error -- catch it in debug?
            return;
        }
        // initialize remainder as dividend
        mod.coeff = Arrays.copyOf(dividend.coeff, dividend.order + 1);


        // XXX make sure divisor[divisor_order] is nonzero
        byte divisorLeading_U = field.log_U[Byte.toUnsignedInt(divisor.coeff[divisor.order])];

        // long division steps along one order at a time, starting at the highest order
        for (int i = dividend.order; Integer.compareUnsigned(i, 0) > 0; i--) {
            // look at the leading coefficient of dividend and divisor
            // if leading coefficient of dividend / leading coefficient of divisor is q
            //   then the next row of subtraction will be q * divisor
            // if order of q < 0 then what we have is the remainder and we are done
            if (Integer.compareUnsigned(i, divisor.order) < 0) {
                break;
            }
            if (Byte.toUnsignedInt(mod.coeff[i]) == 0) {
                continue;
            }
            int qOrder_U = i - divisor.order;
            byte qCoeff_U = (byte) field.fieldDivLog(field.log_U[Byte.toUnsignedInt(mod.coeff[i])], divisorLeading_U);

            // now that we've chosen q, multiply the divisor by q and subtract from
            //   our remainder. subtracting in GF(2^8) is XOR, just like addition
            for (int j = 0; Integer.compareUnsigned(j, divisor.order) <= 0; j++) {
                if (Byte.toUnsignedInt(divisor.coeff[j]) == 0) {
                    continue;
                }
                // all of the multiplication is shifted up by q_order places
                mod.coeff[j + qOrder_U] = field.fieldAdd(mod.coeff[j + qOrder_U],
                        field.fieldMulLogElement(field.log_U[Byte.toUnsignedInt(divisor.coeff[j])], qCoeff_U));

            }
        }
    }

    public static void polynomialFormalDerivative(Field field, Polynomial poly, Polynomial der) {
        // if f(x) = a(n)*x^n + ... + a(1)*x + a(0)
        // then f'(x) = n*a(n)*x^(n-1) + ... + 2*a(2)*x + a(1)
        // where n*a(n) = sum(k=1, n, a(n)) e.g. the nth sum of a(n) in GF(2^8)

        // assumes der.order = poly.order - 1
        Arrays.fill(der.coeff, (byte) 0);
        for (int i = 0; Integer.compareUnsigned(i, der.order) <= 0; i++) {
            // we're filling in the ith power of der, so we look ahead one power in poly
            // f(x) = a(i + 1)*x^(i + 1) -> f'(x) = (i + 1)*a(i + 1)*x^i
            // where (i + 1)*a(i + 1) is the sum of a(i + 1) (i + 1) times, not the product
            der.coeff[i] = field.fieldSum(poly.coeff[i + 1], i + 1);
        }
    }

    public static byte polynomialEval(Field field, Polynomial poly, byte val_U) {
        // evaluate the polynomial poly at a particular element val
        if (Byte.toUnsignedInt(val_U) == 0) {
            return poly.coeff[0];
        }

        byte res_U = 0;

        // we're going to start at 0th order and multiply by val each time
        byte valExponentiated_U = field.log_U[1];
        byte valLog_U = field.log_U[Byte.toUnsignedInt(val_U)];

        for (int i = 0; Integer.compareUnsigned(i, poly.order) <= 0; i++) {
            if (Byte.toUnsignedInt(poly.coeff[i]) != 0) {
                // multiply-accumulate by the next coeff times the next power of val
                res_U = field.fieldAdd(res_U, field.fieldMulLogElement(field.log_U[Byte.toUnsignedInt(poly.coeff[i])],
                                                                              valExponentiated_U));
            }
            // now advance to the next power
            valExponentiated_U = field.fieldMulLog(valExponentiated_U, valLog_U);
        }
        return res_U;
    }

    public byte polynomialEvalLut(Field field, byte[] valExp_U) {
        // evaluate the polynomial poly at a particular element val
        // in this case, all of the logarithms of the successive powers of val have been precalculated
        // this removes the extra work we'd have to do to calculate val_exponentiated each time
        //   if this function is to be called on the same val multiple times
        if (Byte.toUnsignedInt(valExp_U[0]) == 0) {
            return coeff[0];
        }

        byte res_U = 0;

        for (int i = 0; Integer.compareUnsigned(i, order) <= 0; i++) {
            if (Byte.toUnsignedInt(coeff[i]) != 0) {
                // multiply-accumulate by the next coeff times the next power of val
                res_U = field.fieldAdd(res_U, field.fieldMulLogElement(field.log_U[Byte.toUnsignedInt(coeff[i])], valExp_U[i]));
            }
        }
        return res_U;
    }

    public byte polynomialEvalLogLut(Field field, byte[] valExp_U) {
        // evaluate the log_polynomial poly at a particular element val
        // like polynomial_eval_lut, the logarithms of the successive powers of val have been
        //   precomputed
        if (Byte.toUnsignedInt(valExp_U[0]) == 0) {
            if (Byte.toUnsignedInt(coeff[0]) == 0) {
                // special case for the non-existant log case
                return 0;
            }
            return field.exp_U[Byte.toUnsignedInt(coeff[0])];
        }

        byte res_U = 0;

        for (int i = 0; Integer.compareUnsigned(i, order) <= 0; i++) {
            // using 0 as a sentinel value in log -- log(0) is really -inf
            if (Byte.toUnsignedInt(coeff[i]) != 0) {
                // multiply-accumulate by the next coeff times the next power of val
                res_U = field.fieldAdd(res_U, field.fieldMulLogElement(coeff[i], valExp_U[i]));
            }
        }
        return res_U;
    }

    public static void polynomialBuildExpLut(Field field, byte val_U, int order_U, byte[] valExp_U) {
        // create the lookup table of successive powers of val used by polynomial_eval_lut
        byte valExponentiated_U = field.log_U[1];
        byte valLog_U = field.log_U[Byte.toUnsignedInt(val_U)];
        for (int i = 0; Integer.compareUnsigned(i, order_U) <= 0; i++) {
            if (Byte.toUnsignedInt(val_U) == 0) {
                valExp_U[i] = (byte) 0;
            } else {
                valExp_U[i] = valExponentiated_U;
                valExponentiated_U = field.fieldMulLog(valExponentiated_U, valLog_U);
            }
        }
    }

    public static Polynomial polynomialInitFromRoots(Field field,
                                                     int nroots_U,
                                                     byte[] roots_U,
                                                     Polynomial  poly,
                                                     Polynomial[] scratch) {
        Polynomial l = new Polynomial(1);

        // we'll keep two temporary stores of rightside polynomial
        // each time through the loop, we take the previous result and use it as new rightside
        // swap back and forth (prevents the need for a copy)
        Polynomial[] r = new Polynomial[2];
        r[0] = scratch[0];  // clone ?
        r[1] = scratch[1];  // clone ?
        int rcoeffres_U = 0;

        // initialize the result with x + roots[0]
        r[rcoeffres_U].coeff[1] = (byte) 1;
        r[rcoeffres_U].coeff[0] = roots_U[0];
        r[rcoeffres_U].order = 1;

        // initialize lcoeff[1] with x
        // we'll fill in the 0th order term in each loop iter
        l.coeff[1] = (byte) 1;

        // loop through, using previous run's result as the new right hand side
        // this allows us to multiply one group at a time
        for (int i = 1; Integer.compareUnsigned(i, nroots_U) < 0; i++) {
            l.coeff[0] = roots_U[i];
            int nextrcoeff_U = rcoeffres_U;
            rcoeffres_U = Integer.remainderUnsigned(rcoeffres_U + 1, 2);
            r[rcoeffres_U].order = i + 1;
            polynomialMul(field, l, r[nextrcoeff_U], r[rcoeffres_U]);
        }

        poly.coeff = Arrays.copyOf(r[rcoeffres_U].coeff, nroots_U + 1);
        poly.order = nroots_U;

        return poly;
    }

    public static Polynomial polynomialCreateFromRoots(Field field, int nroots, byte[] roots) {
        Polynomial poly = new Polynomial(nroots);
        Polynomial l = new Polynomial(1);

        Polynomial[] r = new Polynomial[2];
        // we'll keep two temporary stores of rightside polynomial
        // each time through the loop, we take the previous result and use it as new rightside
        // swap back and forth (prevents the need for a copy)

        r[0] = new Polynomial(nroots);
        r[1] = new Polynomial(nroots);
        r[0].order = 1;

        int rcoeffres_U = 0;

        // initialize the result with x + roots[0]
        r[rcoeffres_U].coeff[0] = roots[0];
        r[rcoeffres_U].coeff[1] = (byte) 1;

        // initialize lcoeff[1] with x
        // we'll fill in the 0th order term in each loop iter
        l.coeff[1] = (byte) 1;

        // loop through, using previous run's result as the new right hand side
        // this allows us to multiply one group at a time
        for (int i = 1; Integer.compareUnsigned(i, nroots) < 0; i++) {
            l.coeff[0] =  roots[i];
            int nextrCoeff = rcoeffres_U;
            rcoeffres_U = Integer.remainderUnsigned(rcoeffres_U + 1, 2);
            r[rcoeffres_U].order = i + 1;
            polynomialMul(field, l, r[nextrCoeff], r[rcoeffres_U]);
        }

        poly.coeff = Arrays.copyOf(r[rcoeffres_U].coeff, nroots + 1);
        poly.order = nroots;

        return poly;
    }
}

