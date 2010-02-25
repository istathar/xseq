/*
 * XML Sequences for mission critical IT procedures
 *
 * Copyright © 2005 Operational Dynamics
 * Copyright © 2010 Operational Dynamics Consulting, Pty Ltd
 *
 * The code in this file, and the program it is a part of, is made available
 * to you by its authors as open source software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License version
 * 2 ("GPL") as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GPL for more details.
 *
 * You should have received a copy of the GPL along with this program. If not,
 * see http://www.gnu.org/licenses/. The authors of this program may be
 * contacted through http://research.operationaldynamics.com/projects/xseq/.
 */
package generic.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Convert integers to a,b,c....
 * 
 * @author Andrew Cowie
 */
public class AlphaNumberFormat extends Format
{

	public String format(int number) {
		StringBuffer buf = new StringBuffer();
		buf = format(new Integer(number), buf, new FieldPosition(
				NumberFormat.INTEGER_FIELD));
		return buf.toString();
	}

	public StringBuffer format(Object obj, StringBuffer sb, FieldPosition pos) {
		if (!(obj instanceof Number))
			throw new IllegalArgumentException(obj + " must be a Number object");
		if (pos.getField() != NumberFormat.INTEGER_FIELD)
			throw new IllegalArgumentException(pos
					+ " must be FieldPosition(NumberFormat.INTEGER_FIELD");
		int n = ((Number) obj).intValue();
		if (n == 0) {
			throw new IllegalArgumentException("zero is not a legal value for a 1 origin system");
		}
		
		// First, put the digits on a tiny stack. Must be 5 digits.
		for (int i = 0; i < 5; i++) {
			if (n == 0) {
				push(0);
				continue;
			}
			int d = n % 26;
			if (d == 0) {
				push(26);
				n--;
			} else {
				push(d);
			}
			n = n / 26;
		}

		// Now pop and convert.
		for (int i = 0; i < 5; i++) {
			int ch = pop();
			if (ch == 0) {
				continue;
			}
			sb.append((char) (ch - 1 + 'a'));
		}
		return sb;
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

	public Object parseObject(String arg0, ParsePosition arg1) {
		throw new IllegalArgumentException("Parsing not implemented");
	}
}