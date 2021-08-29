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

package net.fabricmc.fabric.api.tag;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.GameEventTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;

import net.fabricmc.fabric.impl.tag.extension.TagFactoryImpl;

/**
 * A factory for accessing datapack tags.
 *
 * <p><b>Note:</b> {@link BuiltinRegistries} does not contain the actual entries that the server has,
 * so comparison would likely fail if you try to use anything from it. Use the actual entry from
 * {@link DynamicRegistryManager}. To warn of this, Fabric will crash if someone tries to call
 * {@link Tag#contains} with entry from {@link BuiltinRegistries}.
 *
 * @see MinecraftServer#getRegistryManager()
 * @see ClientPlayNetworkHandler#getRegistryManager()
 * @see DynamicRegistryManager#get(RegistryKey)
 */
public interface TagFactory<T> {
	TagFactory<Item> ITEM = of(ItemTags::getTagGroup);
	TagFactory<Block> BLOCK = of(BlockTags::getTagGroup);
	TagFactory<Fluid> FLUID = of(FluidTags::getTagGroup);
	TagFactory<GameEvent> GAME_EVENT = of(GameEventTags::getTagGroup);
	TagFactory<EntityType<?>> ENTITY_TYPE = of(EntityTypeTags::getTagGroup);
	TagFactory<Biome> BIOME = of(Registry.BIOME_KEY, "tags/biomes");

	/**
	 * Create a new tag factory for specified registry.
	 *
	 * @param registryKey the key of the registry.
	 * @param dataType    the data type of this tag group, vanilla uses "tags/[plural]" format for built-in groups.
	 */
	static <T> TagFactory<T> of(RegistryKey<? extends Registry<T>> registryKey, String dataType) {
		return TagFactoryImpl.of(registryKey, dataType);
	}

	static <T> TagFactory<T> of(Supplier<TagGroup<T>> tagGroupSupplier) {
		return TagFactoryImpl.of(tagGroupSupplier);
	}

	Tag.Identified<T> create(Identifier id);
}
