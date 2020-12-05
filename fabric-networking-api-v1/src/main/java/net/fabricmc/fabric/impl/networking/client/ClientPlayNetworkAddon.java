/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.networking.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon;
import net.fabricmc.fabric.impl.networking.ChannelInfoHolder;
import net.fabricmc.fabric.impl.networking.NetworkingImpl;

@Environment(EnvType.CLIENT)
public final class ClientPlayNetworkAddon extends AbstractChanneledNetworkAddon<ClientPlayNetworking.PlayChannelHandler> {
	private final ClientPlayNetworkHandler handler;
	private final MinecraftClient client;
	private boolean canSendPackets;

	public ClientPlayNetworkAddon(ClientPlayNetworkHandler handler, MinecraftClient client) {
		super(ClientNetworkingImpl.PLAY, handler.getConnection(), "ClientPlayNetworkAddon for " + handler.getProfile().getName());
		this.handler = handler;
		this.client = client;

		// Must register pending channels via lateinit
		this.registerPendingChannels((ChannelInfoHolder) this.connection);
	}

	// also expose sendRegistration

	public void onServerReady() {
		// Register global receivers
		for (Map.Entry<Identifier, ClientPlayNetworking.PlayChannelHandler> entry : ClientNetworkingImpl.PLAY.getHandlers().entrySet()) {
			this.registerChannel(entry.getKey(), entry.getValue());
		}

		this.sendChannelRegistrationPacket();
		this.canSendPackets = true;

		ClientPlayConnectionEvents.PLAY_INIT.invoker().onPlayInit(this.handler, this, this.client);
		this.receiver.startSession(this);
	}

	/**
	 * Handles an incoming packet.
	 *
	 * @param packet the packet to handle
	 * @return true if the packet has been handled
	 */
	public boolean handle(CustomPayloadS2CPacket packet) {
		PacketByteBuf buf = packet.getData();

		try {
			return this.handle(packet.getChannel(), buf);
		} finally {
			buf.release();
		}
	}

	@Override
	protected void receive(ClientPlayNetworking.PlayChannelHandler handler, PacketByteBuf buf) {
		handler.receive(this.client, this.handler, buf, this);
	}

	// impl details

	@Override
	protected void schedule(Runnable task) {
		MinecraftClient.getInstance().execute(task);
	}

	@Override
	public Packet<?> createPacket(Identifier channelName, PacketByteBuf buf) {
		return ClientPlayNetworking.createC2SPacket(channelName, buf);
	}

	@Override
	protected void invokeRegisterEvent(List<Identifier> ids) {
		S2CPlayChannelEvents.REGISTER.invoker().onChannelRegister(this.handler, this, this.client, ids);
	}

	@Override
	protected void invokeUnregisterEvent(List<Identifier> ids) {
		S2CPlayChannelEvents.UNREGISTER.invoker().onChannelUnregister(this.handler, this, this.client, ids);
	}

	@Override
	protected void handleRegistration(Identifier channelName) {
		// If we can already send packets, immediately send the register packet for this channel
		if (this.canSendPackets) {
			final PacketByteBuf buf = this.createRegistrationPacket(Collections.singleton(channelName));

			if (buf != null) {
				this.sendPacket(NetworkingImpl.REGISTER_CHANNEL, buf);
			}
		}
	}

	@Override
	protected void handleUnregistration(Identifier channelName) {
		// If we can already send packets, immediately send the unregister packet for this channel
		if (this.canSendPackets) {
			final PacketByteBuf buf = this.createRegistrationPacket(Collections.singleton(channelName));

			if (buf != null) {
				this.sendPacket(NetworkingImpl.UNREGISTER_CHANNEL, buf);
			}
		}
	}

	@Override
	public void invokeDisconnectEvent() {
		ClientPlayConnectionEvents.PLAY_DISCONNECT.invoker().onPlayDisconnect(this.handler, this.client);
		this.receiver.endSession(this);
	}

	@Override
	protected boolean isReservedChannel(Identifier channelName) {
		return NetworkingImpl.isReservedPlayChannel(channelName);
	}
}