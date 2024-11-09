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

package top.theillusivec4.curios.common.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.CurioDropsEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.ICuriosMenu;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.CuriosConfig;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;
import top.theillusivec4.curios.common.network.server.SPacketSetIcons;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncCurios;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncData;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncModifiers;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncStack;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncStack.HandlerType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class CuriosEventHandler {

    public static boolean dirtyTags = false;

    private static void handleDrops(String identifier, LivingEntity livingEntity,
                                    List<Tuple<Predicate<ItemStack>, DropRule>> dropRules,
                                    NonNullList<Boolean> renders, IDynamicStackHandler stacks,
                                    boolean cosmetic, Collection<ItemEntity> drops,
                                    boolean keepInventory, LivingDropsEvent evt) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            SlotContext slotContext = new SlotContext(identifier, livingEntity, i, cosmetic,
                    renders.size() > i && renders.get(i));

            if (!stack.isEmpty()) {
                DropRule dropRuleOverride = null;

                for (Tuple<Predicate<ItemStack>, DropRule> override : dropRules) {

                    if (override.getA().test(stack)) {
                        dropRuleOverride = override.getB();
                    }
                }
                DropRule dropRule = dropRuleOverride != null ? dropRuleOverride :
                        CuriosApi.getCurio(stack).map(curio -> curio
                                .getDropRule(slotContext, evt.getSource(), 0, // TODO: Waiting for reimplementing from a [Neo]Forge API'
                                        evt.isRecentlyHit())).orElse(DropRule.DEFAULT);

                if (dropRule == DropRule.DEFAULT) {
                    dropRule = CuriosApi.getSlot(identifier, livingEntity.level()).map(ISlotType::getDropRule)
                            .orElse(DropRule.DEFAULT);
                }

                if ((dropRule == DropRule.DEFAULT && keepInventory) || dropRule == DropRule.ALWAYS_KEEP) {
                    continue;
                }

                if (stack.getEnchantmentLevel(livingEntity.level().holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE)) == 0 && dropRule != DropRule.DESTROY) {
                    drops.add(getDroppedItem(stack, livingEntity));
                }
                stacks.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    private static ItemEntity getDroppedItem(ItemStack droppedItem, LivingEntity livingEntity) {
        double d0 = livingEntity.getY() - 0.30000001192092896D + livingEntity.getEyeHeight();
        ItemEntity entityitem = new ItemEntity(livingEntity.level(), livingEntity.getX(), d0,
                livingEntity.getZ(), droppedItem);
        entityitem.setPickUpDelay(40);
        float f = livingEntity.level().random.nextFloat() * 0.5F;
        float f1 = livingEntity.level().random.nextFloat() * ((float) Math.PI * 2F);
        entityitem.setDeltaMovement((-Mth.sin(f1) * f), 0.20000000298023224D, (Mth.cos(f1) * f));
        return entityitem;
    }

    private static boolean handleMending(Player player, IDynamicStackHandler stacks,
                                         PlayerXpEvent.PickupXp evt) {

        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);

            if (!stack.isEmpty() && stack.getEnchantmentLevel(player.level().holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING)) > 0 &&
                    stack.isDamaged()) {
                evt.setCanceled(true);
                ExperienceOrb orb = evt.getOrb();
                player.takeXpDelay = 2;
                player.take(orb, 1);
                int toRepair = Math.min(orb.value * 2, stack.getDamageValue());
                orb.value -= toRepair / 2;
                stack.setDamageValue(stack.getDamageValue() - toRepair);

                if (orb.value > 0) {
                    player.giveExperiencePoints(orb.value);
                }
                orb.remove(Entity.RemovalReason.KILLED);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent evt) {
        Player playerEntity = evt.getEntity();

        if (playerEntity instanceof ServerPlayer serverPlayer) {
            Collection<ISlotType> slotTypes = CuriosApi.getPlayerSlots(playerEntity).values();
            Map<String, ResourceLocation> icons = new HashMap<>();
            slotTypes.forEach(type -> icons.put(type.getIdentifier(), type.getIcon()));
            PacketDistributor.sendToPlayer(serverPlayer, new SPacketSetIcons(icons));
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent evt) {

        if (evt.getPlayer() == null) {
            PlayerList playerList = evt.getPlayerList();

            for (ServerPlayer player : playerList.getPlayers()) {
                PacketDistributor.sendToPlayer(player,
                        new SPacketSyncData(CuriosSlotManager.getSyncPacket(),
                                CuriosEntityManager.getSyncPacket()));
                CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                    handler.readTag(handler.writeTag());
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new SPacketSyncCurios(player.getId(), handler.getCurios()));

                    if (player.containerMenu instanceof ICuriosMenu curiosContainer) {
                        curiosContainer.resetSlots();
                    }
                });
                Collection<ISlotType> slotTypes = CuriosApi.getPlayerSlots(player).values();
                Map<String, ResourceLocation> icons = new HashMap<>();
                slotTypes.forEach(type -> icons.put(type.getIdentifier(), type.getIcon()));
                PacketDistributor.sendToPlayer(player, new SPacketSetIcons(icons));
            }
        } else {
            ServerPlayer mp = evt.getPlayer();
            PacketDistributor.sendToPlayer(mp, new SPacketSyncData(CuriosSlotManager.getSyncPacket(),
                    CuriosEntityManager.getSyncPacket()));
            CuriosApi.getCuriosInventory(mp).ifPresent(
                    handler -> {
                        handler.readTag(handler.writeTag());
                        PacketDistributor.sendToPlayer(mp,
                                new SPacketSyncCurios(mp.getId(), handler.getCurios()));

                        if (mp.containerMenu instanceof ICuriosMenu curiosContainer) {
                            curiosContainer.resetSlots();
                        }
                    });
            Collection<ISlotType> slotTypes = CuriosApi.getPlayerSlots(mp).values();
            Map<String, ResourceLocation> icons = new HashMap<>();
            slotTypes.forEach(type -> icons.put(type.getIdentifier(), type.getIcon()));
            PacketDistributor.sendToPlayer(mp, new SPacketSetIcons(icons));
        }
    }

    @SubscribeEvent
    public void entityConstructing(EntityEvent.EntityConstructing evt) {
        Entity entity = evt.getEntity();

        if (entity instanceof LivingEntity livingEntity) {
            CuriosApi.getCuriosInventory(livingEntity).ifPresent(inv -> inv.readTag(inv.writeTag()));
        }
    }

    @SubscribeEvent
    public void entityJoinWorld(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();

        if (entity instanceof ServerPlayer serverPlayerEntity) {
            CuriosApi.getCuriosInventory(serverPlayerEntity).ifPresent(handler -> {
                ServerPlayer mp = (ServerPlayer) entity;
                PacketDistributor.sendToPlayer(mp, new SPacketSyncCurios(mp.getId(), handler.getCurios()));
            });
        }
    }

    @SubscribeEvent
    public void playerStartTracking(PlayerEvent.StartTracking evt) {

        Entity target = evt.getTarget();
        Player player = evt.getEntity();

        if (player instanceof ServerPlayer serverPlayer && target instanceof LivingEntity livingBase) {
            CuriosApi.getCuriosInventory(livingBase).ifPresent(
                    handler -> PacketDistributor.sendToPlayer(serverPlayer,
                            new SPacketSyncCurios(target.getId(), handler.getCurios())));
        }
    }

    @SubscribeEvent
    public void playerClone(PlayerEvent.Clone evt) {
        Player player = evt.getEntity();
        Player oldPlayer = evt.getOriginal();
        Optional<ICuriosItemHandler> oldHandler = CuriosApi.getCuriosInventory(oldPlayer);
        Optional<ICuriosItemHandler> newHandler = CuriosApi.getCuriosInventory(player);
        oldHandler.ifPresent(
                oldCurios -> newHandler.ifPresent(newCurios -> newCurios.readTag(oldCurios.writeTag())));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerDrops(LivingDropsEvent evt) {

        LivingEntity livingEntity = evt.getEntity();

        if (!livingEntity.isSpectator()) {

            CuriosApi.getCuriosInventory(livingEntity).ifPresent(handler -> {
                Collection<ItemEntity> drops = evt.getDrops();
                Collection<ItemEntity> curioDrops = new ArrayList<>();
                Map<String, ICurioStacksHandler> curios = handler.getCurios();

                DropRulesEvent dropRulesEvent = new DropRulesEvent(livingEntity, handler, evt.getSource(),
                        0, evt.isRecentlyHit()); // TODO: Waiting for reimplementing from a [Neo]Forge API'
                NeoForge.EVENT_BUS.post(dropRulesEvent);
                List<Tuple<Predicate<ItemStack>, DropRule>> dropRules = dropRulesEvent.getOverrides();
                boolean keepInventory = false;

                if (livingEntity instanceof Player) {
                    keepInventory =
                            livingEntity.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

                    if (CuriosConfig.SERVER.keepCurios.get() != CuriosConfig.KeepCurios.DEFAULT) {
                        keepInventory = CuriosConfig.SERVER.keepCurios.get() == CuriosConfig.KeepCurios.ON;
                    }
                }
                boolean finalKeepInventory = keepInventory;
                curios.forEach((id, stacksHandler) -> {
                    handleDrops(id, livingEntity, dropRules, stacksHandler.getRenders(),
                            stacksHandler.getStacks(), false, curioDrops, finalKeepInventory, evt);
                    handleDrops(id, livingEntity, dropRules, stacksHandler.getRenders(),
                            stacksHandler.getCosmeticStacks(), true, curioDrops, finalKeepInventory, evt);
                });
                CurioDropsEvent dropsEvent = NeoForge.EVENT_BUS.post(
                        new CurioDropsEvent(livingEntity, handler, evt.getSource(), curioDrops,
                                0, evt.isRecentlyHit())); // TODO: Waiting for reimplementing from a [Neo]Forge API'

                if (!dropsEvent.isCanceled()) {
                    drops.addAll(curioDrops);
                }
            });
        }
    }

    @SubscribeEvent
    public void playerXPPickUp(PlayerXpEvent.PickupXp evt) {
        Player player = evt.getEntity();

        if (!player.level().isClientSide) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                Map<String, ICurioStacksHandler> curios = handler.getCurios();
                for (ICurioStacksHandler stacksHandler : curios.values()) {

                    if (handleMending(player, stacksHandler.getStacks(), evt) || handleMending(player,
                            stacksHandler.getCosmeticStacks(), evt)) {
                        return;
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void curioRightClick(PlayerInteractEvent.RightClickItem evt) {
        Player player = evt.getEntity();
        ItemStack stack = evt.getItemStack();
        CuriosApi.getCurio(stack).ifPresent(
                curio -> CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                    Map<String, ICurioStacksHandler> curios = handler.getCurios();
                    Tuple<IDynamicStackHandler, SlotContext> firstSlot = null;

                    for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                        IDynamicStackHandler stackHandler = entry.getValue().getStacks();

                        for (int i = 0; i < stackHandler.getSlots(); i++) {
                            String id = entry.getKey();
                            NonNullList<Boolean> renderStates = entry.getValue().getRenders();
                            SlotContext slotContext = new SlotContext(id, player, i, false,
                                    renderStates.size() > i && renderStates.get(i));

                            if (stackHandler.isItemValid(i, stack) && curio.canEquipFromUse(slotContext)) {
                                ItemStack present = stackHandler.getStackInSlot(i);

                                if (present.isEmpty()) {
                                    stackHandler.setStackInSlot(i, stack.copy());
                                    curio.onEquipFromUse(slotContext);

                                    if (!player.isCreative()) {
                                        int count = stack.getCount();
                                        stack.shrink(count);
                                    }
                                    evt.setCancellationResult(
                                            InteractionResult.sidedSuccess(player.level().isClientSide()));
                                    evt.setCanceled(true);
                                    return;
                                } else if (firstSlot == null) {

                                    if (stackHandler.extractItem(i, stack.getMaxStackSize(), true).getCount() ==
                                            stack.getCount()) {
                                        firstSlot = new Tuple<>(stackHandler, slotContext);
                                    }
                                }
                            }
                        }
                    }

                    if (firstSlot != null) {
                        IDynamicStackHandler stackHandler = firstSlot.getA();
                        SlotContext slotContext = firstSlot.getB();
                        int i = slotContext.index();
                        ItemStack present = stackHandler.getStackInSlot(i);
                        stackHandler.setStackInSlot(i, stack.copy());
                        curio.onEquipFromUse(slotContext);
                        player.setItemInHand(evt.getHand(), present.copy());
                        evt.setCancellationResult(
                                InteractionResult.sidedSuccess(player.level().isClientSide()));
                        evt.setCanceled(true);
                    }
                }));
    }

    @SubscribeEvent
    public void worldTick(LevelTickEvent.Post evt) {

        if (evt.getLevel() instanceof ServerLevel && dirtyTags) {
            PlayerList list = ((ServerLevel) evt.getLevel()).getServer().getPlayerList();

            for (ServerPlayer player : list.getPlayers()) {
                CuriosApi.getCuriosInventory(player).ifPresent(handler -> {

                    for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                        ICurioStacksHandler stacksHandler = entry.getValue();
                        IDynamicStackHandler stacks = stacksHandler.getStacks();
                        IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
                        replaceInvalidStacks(player, stacks);
                        replaceInvalidStacks(player, cosmeticStacks);
                    }
                });
            }
            dirtyTags = false;
        }
    }

    private static void replaceInvalidStacks(ServerPlayer player, IDynamicStackHandler stacks) {

        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);

            if (!stack.isEmpty() && !stacks.isItemValid(i, stack)) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        }
    }

//  @SubscribeEvent
//  public void looting(LootingLevelEvent evt) {
//    DamageSource source = evt.getDamageSource();
//
//    if (source != null && source.getEntity() instanceof LivingEntity living) {
//      evt.setLootingLevel(evt.getLootingLevel() +
//          CuriosApi.getCuriosInventory(living).map(handler -> handler
//              .getLootingLevel(source, evt.getEntity(), evt.getLootingLevel())).orElse(0));
//    }
//  }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreakBlock(BlockDropsEvent event) {
        AtomicInteger experience = new AtomicInteger(event.getDroppedExperience());

        if (experience.get() <= 0 || !(event.getBreaker() instanceof LivingEntity entity))
            return;

        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();
                NonNullList<Boolean> renderStates = entry.getValue().getRenders();

                for (int i = 0; i < stacks.getSlots(); i++) {
                    SlotContext context = new SlotContext(entry.getKey(), entity, i, false,
                            renderStates.size() > i && renderStates.get(i));

                    experience.addAndGet(EnchantmentHelper.processBlockExperience(event.getLevel(), event.getTool(),
                            CuriosApi.getCurio(stacks.getStackInSlot(i)).map(curio -> curio.getFortuneLevel(context, null)).orElse(0)));
                }
            }
        });

        event.setDroppedExperience(experience.get());
    }

    @SubscribeEvent
    public void enderManAnger(final EnderManAngerEvent evt) {
        Player player = evt.getPlayer();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {

            all:
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();

                for (int i = 0; i < stacks.getSlots(); i++) {
                    final int index = i;
                    NonNullList<Boolean> renderStates = entry.getValue().getRenders();
                    boolean hasMask =
                            CuriosApi.getCurio(stacks.getStackInSlot(i)).map(curio -> curio
                                            .isEnderMask(new SlotContext(entry.getKey(), player, index, false,
                                                    renderStates.size() > index && renderStates.get(index)), evt.getEntity()))
                                    .orElse(false);

                    if (hasMask) {
                        evt.setCanceled(true);
                        break all;
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public void tick(EntityTickEvent.Post evt) {
        Entity entity = evt.getEntity();

        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity instanceof Player player &&
                    player.containerMenu instanceof CuriosContainer curiosContainer) {
                curiosContainer.checkQuickMove();
            }

            CuriosApi.getCuriosInventory(livingEntity).ifPresent(handler -> {
                handler.clearCachedSlotModifiers();
                handler.handleInvalidStacks();
                Map<String, ICurioStacksHandler> curios = handler.getCurios();

                for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                    ICurioStacksHandler stacksHandler = entry.getValue();
                    String identifier = entry.getKey();
                    IDynamicStackHandler stackHandler = stacksHandler.getStacks();
                    IDynamicStackHandler cosmeticStackHandler = stacksHandler.getCosmeticStacks();

                    for (int i = 0; i < stacksHandler.getSlots(); i++) {
                        NonNullList<Boolean> renderStates = stacksHandler.getRenders();
                        SlotContext slotContext = new SlotContext(identifier, livingEntity, i, false,
                                renderStates.size() > i && renderStates.get(i));
                        ItemStack stack = stackHandler.getStackInSlot(i);
                        Optional<ICurio> currentCurio = CuriosApi.getCurio(stack);
                        final int index = i;

                        if (!stack.isEmpty()) {
                            stack.inventoryTick(livingEntity.level(), livingEntity, -1, false);
                            currentCurio.ifPresent(curio -> curio.curioTick(slotContext));
                        }

                        if (!livingEntity.level().isClientSide) {
                            ItemStack prevStack = stackHandler.getPreviousStackInSlot(i);

                            if (!ItemStack.matches(stack, prevStack)) {
                                Optional<ICurio> prevCurio = CuriosApi.getCurio(prevStack);
                                syncCurios(livingEntity, stack, currentCurio, prevCurio, identifier, index, false,
                                        renderStates.size() > index && renderStates.get(index), HandlerType.EQUIPMENT);
                                NeoForge.EVENT_BUS
                                        .post(new CurioChangeEvent(livingEntity, identifier, i, prevStack, stack));
                                ResourceLocation id = CuriosApi.getSlotId(slotContext);
                                AttributeMap attributeMap = livingEntity.getAttributes();

                                if (!prevStack.isEmpty()) {
                                    Multimap<Holder<Attribute>, AttributeModifier> map =
                                            CuriosApi.getAttributeModifiers(slotContext, id, prevStack);
                                    Multimap<String, AttributeModifier> slots = HashMultimap.create();
                                    Set<Holder<Attribute>> toRemove = new HashSet<>();

                                    for (Holder<Attribute> attribute : map.keySet()) {

                                        if (attribute.value() instanceof SlotAttribute wrapper) {
                                            slots.putAll(wrapper.getIdentifier(), map.get(attribute));
                                            toRemove.add(attribute);
                                        }
                                    }

                                    for (Holder<Attribute> attribute : toRemove) {
                                        map.removeAll(attribute);
                                    }
                                    map.forEach((key, value) -> {
                                        AttributeInstance attInst = attributeMap.getInstance(key);

                                        if (attInst != null) {
                                            attInst.removeModifier(value);
                                        }
                                    });
                                    handler.removeSlotModifiers(slots);
                                    prevCurio.ifPresent(curio -> curio.onUnequip(slotContext, stack));
                                }

                                if (!stack.isEmpty()) {
                                    Multimap<Holder<Attribute>, AttributeModifier> map =
                                            CuriosApi.getAttributeModifiers(slotContext, id, stack);
                                    Multimap<String, AttributeModifier> slots = HashMultimap.create();
                                    Set<Holder<Attribute>> toRemove = new HashSet<>();

                                    for (Holder<Attribute> attribute : map.keySet()) {

                                        if (attribute.value() instanceof SlotAttribute wrapper) {
                                            slots.putAll(wrapper.getIdentifier(), map.get(attribute));
                                            toRemove.add(attribute);
                                        }
                                    }

                                    for (Holder<Attribute> attribute : toRemove) {
                                        map.removeAll(attribute);
                                    }
                                    map.forEach((key, value) -> {
                                        AttributeInstance attInst = attributeMap.getInstance(key);

                                        if (attInst != null) {
                                            attInst.addTransientModifier(value);
                                        }
                                    });
                                    handler.addTransientSlotModifiers(slots);
                                    currentCurio.ifPresent(curio -> curio.onEquip(slotContext, prevStack));

                                    if (livingEntity instanceof ServerPlayer) {
                                        CuriosRegistry.EQUIP_TRIGGER.get()
                                                .trigger(slotContext, (ServerPlayer) livingEntity, stack);
                                    }
                                }
                                stackHandler.setPreviousStackInSlot(i, stack.copy());
                            }
                            ItemStack cosmeticStack = cosmeticStackHandler.getStackInSlot(i);
                            ItemStack prevCosmeticStack = cosmeticStackHandler.getPreviousStackInSlot(i);

                            if (!ItemStack.matches(cosmeticStack, prevCosmeticStack)) {
                                syncCurios(livingEntity, cosmeticStack,
                                        CuriosApi.getCurio(cosmeticStack),
                                        CuriosApi.getCurio(prevCosmeticStack), identifier, index, true,
                                        true, HandlerType.COSMETIC);
                                cosmeticStackHandler.setPreviousStackInSlot(index, cosmeticStack.copy());
                            }
                            Set<ICurioStacksHandler> updates = handler.getUpdatingInventories();

                            if (!updates.isEmpty()) {
                                PacketDistributor.sendToPlayersTrackingEntityAndSelf(livingEntity,
                                        new SPacketSyncModifiers(livingEntity.getId(), updates));
                                updates.clear();
                            }
                        }
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void livingEquipmentChange(final LivingEquipmentChangeEvent evt) {
        CuriosApi.getCuriosInventory(evt.getEntity()).ifPresent(inv -> {
            ItemStack from = evt.getFrom();
            ItemStack to = evt.getTo();
            EquipmentSlot slot = evt.getSlot();

            if (!from.isEmpty()) {
                Multimap<String, AttributeModifier> slots = HashMultimap.create();
                from.forEachModifier(slot, (att, modifier) -> {
                    if (att.value() instanceof SlotAttribute wrapper) {
                        slots.putAll(wrapper.getIdentifier(), Collections.singleton(modifier));
                    }
                });
                inv.removeSlotModifiers(slots);
            }

            if (!to.isEmpty()) {
                Multimap<String, AttributeModifier> slots = HashMultimap.create();
                to.forEachModifier(slot, (att, modifier) -> {
                    if (att.value() instanceof SlotAttribute wrapper) {
                        slots.putAll(wrapper.getIdentifier(), Collections.singleton(modifier));
                    }
                });
                inv.addTransientSlotModifiers(slots);
            }
        });
    }

    private static void syncCurios(LivingEntity livingEntity, ItemStack stack,
                                   Optional<ICurio> currentCurio, Optional<ICurio> prevCurio,
                                   String identifier, int index, boolean cosmetic, boolean visible,
                                   HandlerType type) {
        SlotContext slotContext = new SlotContext(identifier, livingEntity, index, cosmetic, visible);
        boolean syncable = currentCurio.map(curio -> curio.canSync(slotContext)).orElse(false) ||
                prevCurio.map(curio -> curio.canSync(slotContext)).orElse(false);
        CompoundTag syncTag = syncable ? currentCurio.map(curio -> {
            CompoundTag tag = curio.writeSyncData(slotContext);
            return tag != null ? tag : new CompoundTag();
        }).orElse(new CompoundTag()) : new CompoundTag();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(livingEntity,
                new SPacketSyncStack(livingEntity.getId(), identifier, index, stack, type.ordinal(),
                        syncTag));
    }
}
