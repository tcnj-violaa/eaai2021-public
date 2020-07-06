/*
Copyright (C) 2020 Jason Hiebel

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

Information about the GNU General Public License is available online at:
  http://www.gnu.org/licenses/
To receive a copy of the GNU General Public License, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
02111-1307, USA.
*/

package eaai.ginrummy.util;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A stubbed version of a print stream which ignores all output issued through
 * print commands. The underlying {@link OutputStream} voids data by performing
 * no write behavior.
 *
 * @author Jason Hiebel
 */
public class StubPrintStream extends PrintStream {
	/**
	 * Creates a new print stream which ignores all output, for the purpose of
	 * disabling output (such as through reseting System.out or System.err)
	 * without requiring conditional execution of those output commands.
	 *
	 * @see PrintStream#PrintStream(OutputStream)
	 */
	 public StubPrintStream() {
		 super(new OutputStream() { @Override public void write(int b) {} });
	 }
}
