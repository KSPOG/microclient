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

import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.runelite.api.Client;

/**
 * Owns the lifecycle boundary between the RuneLite host and the patched game
 * client supplied by the {@code injected-client} runtime dependency.
 *
 * <p>The loader is invoked at most once. Loading and initialization are
 * intentionally separate because RuneLite must inject callback and service
 * members into the client before the game engine is initialized.</p>
 */
public final class InjectedClientRuntime
{
	public enum State
	{
		NEW,
		LOADING,
		LOADED,
		INITIALIZING,
		INITIALIZED,
		FAILED
	}

	private final Supplier<Client> clientLoader;

	private State state = State.NEW;
	private Client client;
	private String clientClassName;
	private String buildId;
	private Throwable failure;

	public InjectedClientRuntime(Supplier<Client> clientLoader)
	{
		this.clientLoader = Objects.requireNonNull(clientLoader, "clientLoader");
	}

	/**
	 * Loads the patched client once and returns the same instance thereafter.
	 *
	 * @return injected RuneLite client
	 */
	public synchronized Client load()
	{
		if (client != null)
		{
			return client;
		}

		if (state == State.FAILED)
		{
			throw new IllegalStateException("Injected client runtime has failed", failure);
		}

		if (state == State.LOADING || state == State.INITIALIZING)
		{
			throw new IllegalStateException("Re-entrant injected client load while state is " + state);
		}

		state = State.LOADING;
		try
		{
			Client loadedClient = Objects.requireNonNull(clientLoader.get(), "clientLoader returned null");
			client = loadedClient;
			clientClassName = loadedClient.getClass().getName();
			buildId = loadedClient.getBuildID();
			state = State.LOADED;
			return loadedClient;
		}
		catch (RuntimeException | Error ex)
		{
			fail(ex);
			throw ex;
		}
	}

	/**
	 * Initializes the already member-injected client. Repeated calls are safe.
	 */
	public synchronized void initialize()
	{
		Client loadedClient = load();
		if (state == State.INITIALIZED)
		{
			return;
		}

		if (state != State.LOADED)
		{
			throw new IllegalStateException("Cannot initialize injected client while state is " + state);
		}

		state = State.INITIALIZING;
		try
		{
			loadedClient.initialize();
			state = State.INITIALIZED;
		}
		catch (RuntimeException | Error ex)
		{
			fail(ex);
			throw ex;
		}
	}

	public synchronized State getState()
	{
		return state;
	}

	@Nullable
	public synchronized String getClientClassName()
	{
		return clientClassName;
	}

	@Nullable
	public synchronized String getBuildId()
	{
		return buildId;
	}

	@Nullable
	public synchronized Throwable getFailure()
	{
		return failure;
	}

	private void fail(Throwable throwable)
	{
		failure = throwable;
		state = State.FAILED;
	}
}
