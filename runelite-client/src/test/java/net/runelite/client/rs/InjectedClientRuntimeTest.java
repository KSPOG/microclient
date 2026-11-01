/*
 * Copyright (c) 2026 Microbot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.rs;

import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InjectedClientRuntimeTest
{
	@Test
	public void loadsClientOnlyOnce()
	{
		Client client = mock(Client.class);
		when(client.getBuildID()).thenReturn("revision-1");
		AtomicInteger loadCount = new AtomicInteger();
		InjectedClientRuntime runtime = new InjectedClientRuntime(() ->
		{
			loadCount.incrementAndGet();
			return client;
		});

		assertSame(client, runtime.load());
		assertSame(client, runtime.load());
		assertEquals(1, loadCount.get());
		assertEquals(InjectedClientRuntime.State.LOADED, runtime.getState());
		assertEquals(client.getClass().getName(), runtime.getClientClassName());
		assertEquals("revision-1", runtime.getBuildId());
	}

	@Test
	public void initializesClientOnlyOnce()
	{
		Client client = mock(Client.class);
		InjectedClientRuntime runtime = new InjectedClientRuntime(() -> client);

		runtime.initialize();
		runtime.initialize();

		verify(client).initialize();
		assertEquals(InjectedClientRuntime.State.INITIALIZED, runtime.getState());
	}

	@Test
	public void recordsLoadFailure()
	{
		RuntimeException expected = new RuntimeException("load failed");
		InjectedClientRuntime runtime = new InjectedClientRuntime(() ->
		{
			throw expected;
		});

		RuntimeException actual = null;
		try
		{
			runtime.load();
		}
		catch (RuntimeException ex)
		{
			actual = ex;
		}

		assertSame(expected, actual);
		assertEquals(InjectedClientRuntime.State.FAILED, runtime.getState());
		assertSame(expected, runtime.getFailure());
	}

	@Test
	public void recordsInitializationFailure()
	{
		Client client = mock(Client.class);
		RuntimeException expected = new RuntimeException("initialization failed");
		doThrow(expected).when(client).initialize();
		InjectedClientRuntime runtime = new InjectedClientRuntime(() -> client);

		RuntimeException actual = null;
		try
		{
			runtime.initialize();
		}
		catch (RuntimeException ex)
		{
			actual = ex;
		}

		assertSame(expected, actual);
		assertEquals(InjectedClientRuntime.State.FAILED, runtime.getState());
		assertSame(expected, runtime.getFailure());
	}
}
