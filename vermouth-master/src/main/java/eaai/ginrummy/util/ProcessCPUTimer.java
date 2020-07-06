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

import java.lang.management.*;
import com.sun.management.OperatingSystemMXBean;
import java.util.concurrent.TimeoutException;

/**
 * A utility class for measuring the CPU time of the underlying JVM process and
 * executing a thread with a restricted process CPU time. This operation may not
 * be supported in all JVMs, although a cursory test suggests that it is
 * currently supported by official JVM releases for 1.6 and 1.7 on all major
 * platforms.
 * <p>
 * As competitors are generally free to create their own threads for
 * computation, we need to be sure that we are accounting for every thread's CPU
 * time. The execute function has a slight, unavoidable overhead for this
 * calculation due to polling.
 * <p>
 * For fair results it is recommended that thread use is minimized during the
 * timing process.
 * <p>
 * The alternative to this timer would be a thread based timer. There are many
 * difficulties to thread based CPU timing. Threads only report a CPU time while
 * they are running, so it would be possible for short-lived threads to escape
 * the timing function. Polling periodically for thread CPU times would produce
 * a strict lower estimate on the actual time as well, since the time between
 * the last poll and the thread's actual completion would be immesurable.
 *
 * @author Jason Hiebel
 */
public class ProcessCPUTimer {
	private static final OperatingSystemMXBean mxOS = ((OperatingSystemMXBean) (ManagementFactory.getOperatingSystemMXBean()));

	// static class, private constructor
	private ProcessCPUTimer() { }

	/**
	 * Calculates the CPU time of the underlying JVM process.
	 * <p>
	 * The time is returned in nanosecond precision but not necessarily
	 * nanosecond accuracy. If CPU time is not supported for the underlying JVM
	 * this method will return -1.
	 *
	 * @return the CPU time of the underlying JVM process; -1 is unsupported
	 */
	public static long time() {
		return mxOS.getProcessCpuTime();
	}

	/**
	 * Determines if the underlying JVM process supports CPU time.
	 *
	 * @return true if CPU time is supported
	 */
	public static boolean isSupported() {
		return time() == -1;
	}

	/**
	 * Executes the given runnable in an new thread. The parent thread will
	 * periodically poll for the CPU time of the JVM process and verify that the
	 * alive time for the child thread has not surpassed the timeout. This
	 * function will either return silently if the child thread completes before
	 * the timeout or will otherwise throw a TimeoutException.
	 * <p>
	 * In order to enforce a timeout this function may be required to stop a
	 * thread. It is the responsibility of the caller to ensure that any held
	 * resources will not impair the function of the remainder of the program.
	 * As correctness is required by the caller, we suppress the deprecation
	 * here.
	 *
	 * @param runnable the function to time
	 * @param timeout the timeout, in milliseconds
	 * @param poll the polling rate, in milliseconds
	 * @throws TimeoutException if the runnable results in a timeout
	 */
	@SuppressWarnings("deprecation")
	public static void execute(final Runnable runnable, final long timeout, final long poll) throws TimeoutException {
		Thread thread = new Thread(runnable);
		thread.start();

		long start = time();
		while (true) {
			try { thread.join(poll); } catch (InterruptedException e) { }

			if (time() - start >= timeout * 1000000) {
				if (thread.isAlive()) { thread.stop(); }
				throw new TimeoutException();
			}
			else if (!thread.isAlive()) { break; }
		}
	}
}
