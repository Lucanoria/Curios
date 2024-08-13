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

package top.theillusivec4.curios.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.util.ISlotHelper;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

public class SlotHelper implements ISlotHelper {

  private final Map<String, ISlotType> idToType = new HashMap<>();

  @Override
  public void addSlotType(ISlotType slotType) {
    this.idToType.put(slotType.getIdentifier(), slotType);
  }

  @Override
  public void clear() {
    this.idToType.clear();
  }

  @Override
  public Optional<ISlotType> getSlotType(String identifier) {
    return Optional.ofNullable(this.idToType.get(identifier));
  }

  @Override
  public Collection<ISlotType> getSlotTypes() {
    return Collections.unmodifiableCollection(idToType.values());
  }

  @Override
  public Collection<ISlotType> getSlotTypes(LivingEntity livingEntity) {
    return getSlotTypes();
  }

  @Override
  public Set<String> getSlotTypeIds() {
    return Collections.unmodifiableSet(idToType.keySet());
  }

  @Override
  public int getSlotsForType(@Nonnull final LivingEntity livingEntity, String identifier) {
    return 0;
  }

  @Override
  public void setSlotsForType(String id, final LivingEntity livingEntity, int amount) {
    // no op
  }
}
