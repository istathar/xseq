/*
 * Copyright (c) Ian F. Darwin, ian@darwinsys.com, 1996-2004.
 * All rights reserved. Software written by Ian F. Darwin and others.
 * $Id: license.html,v 1.3 2004/01/03 02:09:44 ian Exp $
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the author nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Java, the Duke mascot, and all variants of Sun's Java "steaming coffee
 * cup" logo are trademarks of Sun Microsystems. Sun's, and James Gosling's,
 * pioneering role in inventing and promulgating (and standardizing) the Java 
 * language and environment is gratefully acknowledged.
 * 
 * The pioneering role of Dennis Ritchie and Bjarne Stroustrup, of AT&T, for
 * inventing predecessor languages C and C++ is also gratefully acknowledged.
 */
package com.darwinsys;

import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Roman Number class. Not localized, since "Latin's a Dead Dead Language..."
 * and we don't display Roman Numbers differently in different Locales. Filled
 * with quick-n-dirty algorithms.
 * 
 * @author Ian F. Darwin, ian@darwinsys.com
 * @version $Id: RomanNumberFormat.java,v 1.5 2001/04/07 01:35:54 ian Exp $
 */
public class RomanNumberFormat extends Format
{

	/** Characters used in "Arabic to Roman", that is, format() methods. */
	static char	A2R[][]	= {
			{
			0, 'M'
			}, {
			0, 'C', 'D', 'M'
			}, {
			0, 'X', 'L', 'C'
			}, {
			0, 'I', 'V', 'X'
			},
						};

	/**
	 * Format a given double as a Roman Numeral; just truncate to a long, and
	 * call format(long).
	 */
	public String format(double n) {
		return format((int) n);
	}

	/**
	 * Format a given long as a Roman Numeral. Just call the three-argument
	 * form.
	 */
	public String format(int n) {
		if (n < 0 || n >= 4000)
			throw new NumberFormatException(n + " must be >= 0 && < 4000");
		StringBuffer sb = new StringBuffer();
		format(new Integer((int) n), sb, new FieldPosition(
				NumberFormat.INTEGER_FIELD));
		return sb.toString();
	}

	/*
	 * Format the given Number as a Roman Numeral, returning the Stringbuffer
	 * (updated), and updating the FieldPosition. This method is the REAL
	 * FORMATTING ENGINE. Method signature is overkill, but required as a
   	 * subclass of Format.
	 */
	public StringBuffer format(Object on, StringBuffer sb, FieldPosition fp) {
		if (!(on instanceof Number))
			throw new IllegalArgumentException(on + " must be a Number object");
		if (fp.getField() != NumberFormat.INTEGER_FIELD)
			throw new IllegalArgumentException(fp
					+ " must be FieldPosition(NumberFormat.INTEGER_FIELD");
		int n = ((Number) on).intValue();

		// First, put the digits on a tiny stack. Must be 4 digits.
		for (int i = 0; i < 4; i++) {
			int d = n % 10;
			push(d);
			// System.out.println("Pushed " + d);
			n = n / 10;
		}

		// Now pop and convert.
		for (int i = 0; i < 4; i++) {
			int ch = pop();
			// System.out.println("Popped " + ch);
			if (ch == 0)
				continue;
			else if (ch <= 3) {
				for (int k = 1; k <= ch; k++)
					sb.append(A2R[i][1]); // I
			} else if (ch == 4) {
				sb.append(A2R[i][1]); // I
				sb.append(A2R[i][2]); // V
			} else if (ch == 5) {
				sb.append(A2R[i][2]); // V
			} else if (ch <= 8) {
				sb.append(A2R[i][2]); // V
				for (int k = 6; k <= ch; k++)
					sb.append(A2R[i][1]); // I
			} else { // 9
				sb.append(A2R[i][1]);
				sb.append(A2R[i][3]);
			}
		}
		// fp.setBeginIndex(0);
		// fp.setEndIndex(3);
		return sb;
	}

	/** Parse a generic object, returning an Object */
	public Object parseObject(String what, ParsePosition where) {
		throw new IllegalArgumentException("Parsing not implemented");
	}

	/* Implement a toy stack */
	protected int	stack[]	= new int[10];
	protected int	depth	= 0;

	/* Implement a toy stack */
	protected void push(int n) {
		stack[depth++] = n;
	}

	/* Implement a toy stack */
	protected int pop() {
		return stack[--depth];
	}
}