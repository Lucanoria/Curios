/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package top.theillusivec4.curios.api.type.data;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.conditions.ICondition;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Used in data generation to represent the slot data
 */
public interface ISlotData {

    ISlotData replace(boolean replace);

    ISlotData order(int order);

    ISlotData size(int size);

    ISlotData operation(AttributeModifier.Operation operation);

    ISlotData useNativeGui(boolean useNativeGui);

    ISlotData addCosmetic(boolean addCosmetic);

    ISlotData renderToggle(boolean renderToggle);

    ISlotData icon(ResourceLocation icon);

    ISlotData dropRule(ICurio.DropRule dropRule);

    ISlotData addCondition(ICondition condition);

    ISlotData addValidator(ResourceLocation resourceLocation);

    JsonObject serialize(HolderLookup.Provider provider);
}
