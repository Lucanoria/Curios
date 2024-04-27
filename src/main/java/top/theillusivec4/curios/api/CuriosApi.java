/*
 * Copyright (c) 2018-2020 C4
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
 */

package top.theillusivec4.curios.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;
import top.theillusivec4.curios.api.type.util.IIconHelper;
import top.theillusivec4.curios.api.type.util.ISlotHelper;

public final class CuriosApi {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final String MODID = "curios";

  /**
   * Registers a {@link ICurioItem} instance to an item.
   * <br>
   * This will override any existing {@link ICurioItem} interfaces implemented on an item, however
   * it will NOT override {@link ICurio} instances initialized in {@link net.minecraftforge.common.extensions.IForgeItem#initCapabilities(ItemStack, CompoundTag)}.
   *
   * @param item  The item to register the ICurio instance to
   * @param curio The ICurio instance that provides curio behavior for the item
   */
  public static void registerCurio(Item item, ICurioItem curio) {
    apiError();
  }

  /**
   * Gets the registered slot type for the identifier, if it exists, on the given level.
   *
   * @param id    The slot type identifier
   * @param level The level for the slot type
   * @return The registered slot type or empty if it doesn't exist
   */
  public static Optional<ISlotType> getSlot(String id, Level level) {
    return CuriosApi.getSlot(id, level.isClientSide());
  }

  /**
   * Gets the registered slot type for the identifier, if it exists, on the given side.
   *
   * @param id       The slot type identifier
   * @param isClient True for client-side slots, false for server-side slots
   * @return The registered slot type or empty if it doesn't exist
   */
  public static Optional<ISlotType> getSlot(String id, boolean isClient) {
    return Optional.ofNullable(CuriosApi.getSlots(isClient).get(id));
  }

  /**
   * Gets all the registered slot types on the given level.
   *
   * @param level The level for the slot type
   * @return The registered slot types
   */
  public static Map<String, ISlotType> getSlots(Level level) {
    return CuriosApi.getSlots(level.isClientSide());
  }

  /**
   * Gets all the registered slot types on the given side.
   *
   * @param isClient True for client-side slots, false for server-side slots
   * @return The registered slot types
   */
  public static Map<String, ISlotType> getSlots(boolean isClient) {
    apiError();
    return Map.of();
  }

  /**
   * Gets all the registered slot types provided to player entities on the given level.
   *
   * @param level The level for the slot types
   * @return The slot types provided to player entities
   */
  public static Map<String, ISlotType> getPlayerSlots(Level level) {
    return CuriosApi.getPlayerSlots(level.isClientSide());
  }

  /**
   * Gets all the registered slot types provided to player entities on the given side.
   *
   * @param isClient True for client-side slots, false for server-side slots
   * @return The slot types provided to player entities
   */
  public static Map<String, ISlotType> getPlayerSlots(boolean isClient) {
    return CuriosApi.getEntitySlots(EntityType.PLAYER, isClient);
  }

  /**
   * Gets all the registered slot types provided the player entity.
   *
   * @param player The {@link Player} for the slot types
   * @return The slot types provided to the player entity
   */
  public static Map<String, ISlotType> getPlayerSlots(Player player) {
    return CuriosApi.getEntitySlots(player);
  }

  /**
   * Gets all the registered slot types provided to an entity.
   *
   * @param livingEntity The {@link LivingEntity} for the slot types
   * @return The slot types provided to the entity
   */
  public static Map<String, ISlotType> getEntitySlots(LivingEntity livingEntity) {
    return livingEntity != null ?
        CuriosApi.getEntitySlots(livingEntity.getType(), livingEntity.level()) : Map.of();
  }

  /**
   * Gets all the registered slot types provided to an entity type on the given level.
   *
   * @param type The entity type for the slot types
   * @return The slot types provided to the entity type
   */
  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, Level level) {
    return CuriosApi.getEntitySlots(type, level.isClientSide());
  }

  /**
   * Gets all the registered slot types provided to an entity.
   *
   * @param type     The entity type for the slot types
   * @param isClient True for client-side slots, false for server-side slots
   * @return The slot types provided to the entity
   */
  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, boolean isClient) {
    apiError();
    return Map.of();
  }

  /**
   * Gets all the registered slot types for the provided ItemStack and level.
   *
   * @param stack The ItemStack for the slot types
   * @param level The level for the ItemStack
   * @return The slot types for the provided ItemStack
   */
  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, Level level) {
    return CuriosApi.getItemStackSlots(stack, level.isClientSide());
  }

  /**
   * Gets all the registered slot types for the provided ItemStack and level.
   *
   * @param stack    The ItemStack for the slot types
   * @param isClient True for client-side slots, false for server-side slots
   * @return The slot types for the provided ItemStack
   */
  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, boolean isClient) {
    apiError();
    return Map.of();
  }

  /**
   * Gets all the registered slot types for the provided ItemStack and entity.
   *
   * @param stack        The ItemStack for the slot types
   * @param livingEntity The entity with the slot types
   * @return The slot types for the provided ItemStack and entity
   */
  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack,
                                                         LivingEntity livingEntity) {
    apiError();
    return Map.of();
  }

  /**
   * Gets a {@link LazyOptional} of the curio capability attached to the {@link ItemStack}.
   *
   * @param stack The {@link ItemStack} to get the curio capability from
   * @return {@link LazyOptional} of the curio capability
   */
  public static LazyOptional<ICurio> getCurio(ItemStack stack) {
    apiError();
    return LazyOptional.empty();
  }

  /**
   * Creates a new {@link ICapabilityProvider} for the given {@link ICurio} instance, to be used in
   * capability initialization.
   *
   * @param curio The ICurio implementation to use
   * @return The ICapabilityProvider that provides the ICurio implementation
   */
  @Nonnull
  public static ICapabilityProvider createCurioProvider(final ICurio curio) {
    CuriosApi.apiError();
    return Items.AIR.getDefaultInstance();
  }

  /**
   * Gets a {@link LazyOptional} of the curio inventory capability attached to the entity.
   *
   * @param livingEntity The {@link LivingEntity} to get the curio inventory capability from
   * @return {@link LazyOptional} of the curio inventory capability
   */
  public static LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity livingEntity) {
    apiError();
    return LazyOptional.empty();
  }

  /**
   * Checks if the ItemStack is valid for a particular stack and slot context.
   *
   * @param slotContext Context about the slot that the ItemStack is being checked for
   * @param stack       The ItemStack in question
   * @return True if the ItemStack is valid for the slot, false otherwise
   */
  public static boolean isStackValid(SlotContext slotContext, ItemStack stack) {
    apiError();
    return false;
  }

  /**
   * Retrieves a map of attribute modifiers for the ItemStack.
   * <br>
   * Note that only the identifier is guaranteed to be present in the slot context. For instances
   * where the ItemStack may not be in a curio slot, such as when retrieving item tooltips, the
   * index is -1 and the wearer may be null.
   *
   * @param slotContext Context about the slot that the ItemStack is equipped in or may potentially
   *                    be equipped in
   * @param uuid        Slot-unique UUID
   * @param stack       The ItemStack in question
   * @return A map of attribute modifiers
   */
  public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, UUID uuid, ItemStack stack) {
    apiError();
    return HashMultimap.create();
  }

  /**
   * Adds a slot modifier to a specified attribute map.
   *
   * @param map        A {@link Multimap} of attributes to attribute modifiers
   * @param identifier The identifier of the slot to add the modifier onto
   * @param uuid       A UUID associated wth the slot
   * @param amount     The amount of the modifier
   * @param operation  The operation of the modifier
   */
  public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String identifier,
                                     UUID uuid, double amount,
                                     AttributeModifier.Operation operation) {
    apiError();
  }

  /**
   * Adds a slot modifier to an ItemStack's tag data.
   *
   * @param stack      The ItemStack to add the modifier to
   * @param identifier The identifier of the slot to add the modifier onto
   * @param name       The name for the modifier
   * @param uuid       A UUID associated wth the modifier, or null if the slot UUID should be used
   * @param amount     The amount of the modifier
   * @param operation  The operation of the modifier
   * @param slot       The slot that the ItemStack provides the modifier from
   */
  public static void addSlotModifier(ItemStack stack, String identifier, String name, UUID uuid,
                                     double amount, AttributeModifier.Operation operation,
                                     String slot) {
    apiError();
  }

  /**
   * Adds an attribute modifier to an ItemStack's tag data.
   *
   * @param stack     The ItemStack to add the modifier to
   * @param attribute The attribute to add the modifier onto
   * @param name      The name for the modifier
   * @param uuid      A UUID associated wth the modifier, or null if the slot UUID should be used
   * @param amount    The amount of the modifier
   * @param operation The operation of the modifier
   * @param slot      The slot that the ItemStack provides the modifier from
   */
  public static void addModifier(ItemStack stack, Attribute attribute, String name, UUID uuid,
                                 double amount, AttributeModifier.Operation operation,
                                 String slot) {
    apiError();
  }

  /**
   * Registers a new predicate keyed to a {@link ResourceLocation} for deciding which slots are
   * assigned to a given {@link ItemStack}.
   *
   * @param resourceLocation The unique {@link ResourceLocation} of the validator
   * @param predicate        The predicate to register for a given stack and {@link SlotResult}
   */
  public static void registerCurioPredicate(ResourceLocation resourceLocation,
                                            Predicate<SlotResult> predicate) {
    apiError();
  }

  /**
   * Gets an existing predicate, or empty if none found, keyed to a {@link ResourceLocation} for
   * deciding which slots are assigned to a given {@link ItemStack}.
   *
   * @param resourceLocation The unique {@link ResourceLocation} of the validator
   * @return An Optional of the predicate found for the ResourceLocation, or empty otherwise
   */
  public static Optional<Predicate<SlotResult>> getCurioPredicate(
      ResourceLocation resourceLocation) {
    apiError();
    return Optional.empty();
  }

  /**
   * Gets all registered predicates deciding which slots are assigned to a given {@link ItemStack}.
   *
   * @return A map of the registered predicates keyed by {@link ResourceLocation}
   */
  public static Map<ResourceLocation, Predicate<SlotResult>> getCurioPredicates() {
    apiError();
    return Map.of();
  }

  /**
   * Evaluates a set of predicates to determine if a given {@link SlotResult} is a valid assignment.
   *
   * @param predicates A set of ResourceLocations representing the predicates to iterate
   * @param slotResult The SlotResult containing the {@link SlotContext} and {@link ItemStack}
   * @return True if any of the predicates pass, false otherwise
   */
  public static boolean testCurioPredicates(Set<ResourceLocation> predicates,
                                            SlotResult slotResult) {
    apiError();
    return true;
  }

  /**
   * Gets a UUID based on the provided {@link SlotContext}.
   *
   * @param slotContext The SlotContext to base the UUID on
   * @return The UUID based on the SlotContext
   */
  public static UUID getSlotUuid(SlotContext slotContext) {
    apiError();
    return UUID.randomUUID();
  }

  /**
   * Performs breaking behavior used from the single-input consumer in {@link ItemStack#hurtAndBreak(int, LivingEntity, Consumer)}
   * <br>
   * This will be necessary in order to trigger break animations in curio slots
   * <br>
   * Example: { stack.hurtAndBreak(amount, entity, damager -> CuriosApi.broadcastCurioBreakEvent(slotContext)); }
   *
   * @param slotContext Context about the slot that the curio is in
   */
  public static void broadcastCurioBreakEvent(SlotContext slotContext) {
    apiError();
  }

  static void apiError() {
    LOGGER.error("Missing Curios API implementation!");
  }

  // ========= DEPRECATED =============

  /**
   * @deprecated See {@link CuriosApi#getSlot(String, Level)}
   * and {@link CuriosApi#getSlot(String, boolean)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Optional<ISlotType> getSlot(String id) {
    return CuriosApi.getSlot(id, false);
  }

  /**
   * @deprecated See {@link CuriosApi#getSlot(String, Level)} and {@link ISlotType#getIcon()}.
   */
  @Nonnull
  public static ResourceLocation getSlotIcon(String id) {
    return CuriosApi.getSlot(id, true).map(ISlotType::getIcon)
        .orElse(new ResourceLocation(CuriosApi.MODID, "slot/empty_curio_slot"));
  }

  /**
   * @deprecated See {@link CuriosApi#getSlots(Level)} and {@link CuriosApi#getSlots(boolean)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getSlots() {
    return CuriosApi.getSlots(false);
  }

  /**
   * @deprecated See {@link CuriosApi#getEntitySlots(EntityType, Level)},
   * {@link CuriosApi#getEntitySlots(EntityType, boolean)},
   * and {@link CuriosApi#getEntitySlots(LivingEntity)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type) {
    return CuriosApi.getEntitySlots(type, false);
  }

  /**
   * @deprecated See {@link CuriosApi#getPlayerSlots(Level)},
   * {@link CuriosApi#getPlayerSlots(boolean)}, and {@link CuriosApi#getPlayerSlots(Player)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getPlayerSlots() {
    return CuriosApi.getPlayerSlots(false);
  }

  /**
   * @deprecated See {@link CuriosApi#getItemStackSlots(ItemStack, Level)}
   * and {@link CuriosApi#getItemStackSlots(ItemStack, boolean)}
   */
  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack) {
    return CuriosApi.getItemStackSlots(stack, FMLLoader.getDist() == Dist.CLIENT);
  }

  private static IIconHelper iconHelper;

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static void setIconHelper(IIconHelper helper) {

    if (iconHelper == null) {
      iconHelper = helper;
    }
  }

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static IIconHelper getIconHelper() {
    return iconHelper;
  }

  private static ICuriosHelper curiosHelper;

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static void setCuriosHelper(ICuriosHelper helper) {

    if (curiosHelper == null) {
      curiosHelper = helper;
    }
  }

  @Deprecated(since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static ICuriosHelper getCuriosHelper() {
    return curiosHelper;
  }

  private static ISlotHelper slotHelper;

  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static ISlotHelper getSlotHelper() {
    return slotHelper;
  }

  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static void setSlotHelper(ISlotHelper helper) {
    slotHelper = helper;
  }
}
