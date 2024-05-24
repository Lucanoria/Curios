package top.theillusivec4.curios.common.inventory.container;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.CuriosConfig;
import top.theillusivec4.curios.common.CuriosRegistry;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.server.SPacketPage;
import top.theillusivec4.curios.common.network.server.SPacketQuickMove;

public class CuriosContainerV2 extends CuriosContainer {

  private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[] {
      InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
      InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
  private static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[] {
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
      EquipmentSlot.FEET};

  public final ICuriosItemHandler curiosHandler;
  public final Player player;

  private final boolean isLocalWorld;

  private final CraftingContainer craftMatrix = new TransientCraftingContainer(this, 2, 2);
  private final ResultContainer craftResult = new ResultContainer();
  public int currentPage;
  public int totalPages;
  public List<Integer> grid = new ArrayList<>();
  private final List<ProxySlot> proxySlots = new ArrayList<>();
  private int moveToPage = -1;
  private int moveFromIndex = -1;
  public boolean hasCosmetics;
  public boolean isViewingCosmetics;
  public int panelWidth;

  public CuriosContainerV2(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
    this(windowId, playerInventory);
  }

  public CuriosContainerV2(int windowId, Inventory playerInventory) {
    super(windowId, playerInventory);
    this.menuType = CuriosRegistry.CURIO_MENU_NEW.get();
    this.player = playerInventory.player;
    this.isLocalWorld = this.player.level().isClientSide;
    this.curiosHandler = CuriosApi.getCuriosInventory(this.player).orElse(null);
    this.resetSlots();
  }

  public void setPage(int page) {
    this.slots.clear();
    this.lastSlots.clear();
    this.remoteSlots.clear();
    this.panelWidth = 0;
    int visibleSlots = 0;
    int maxSlotsPerPage = CuriosConfig.SERVER.maxSlotsPerPage.get();
    int startingIndex = page * maxSlotsPerPage;
    int columns = 0;

    if (this.curiosHandler != null) {
      visibleSlots = this.curiosHandler.getVisibleSlots();
      int slotsOnPage = Math.min(maxSlotsPerPage, visibleSlots - startingIndex);
      int calculatedColumns = (int) Math.ceil((double) slotsOnPage / 8);
      int minimumColumns = Math.min(slotsOnPage, CuriosConfig.SERVER.minimumColumns.get());
      columns = Mth.clamp(calculatedColumns, minimumColumns, 8);
      this.panelWidth = 14 + 18 * columns;
    }
    this.addSlot(
        new ResultSlot(player, this.craftMatrix, this.craftResult, 0, 154, 28));

    for (int i = 0; i < 2; ++i) {

      for (int j = 0; j < 2; ++j) {
        this.addSlot(new Slot(this.craftMatrix, j + i * 2, 98 + j * 18, 18 + i * 18));
      }
    }

    for (int k = 0; k < 4; ++k) {
      final EquipmentSlot equipmentslottype = VALID_EQUIPMENT_SLOTS[k];
      this.addSlot(new Slot(player.getInventory(), 36 + (3 - k), 8, 8 + k * 18) {
        @Override
        public void set(@Nonnull ItemStack stack) {
          ItemStack itemstack = this.getItem();
          super.set(stack);
          CuriosContainerV2.this.player.onEquipItem(equipmentslottype, itemstack, stack);
        }

        @Override
        public int getMaxStackSize() {
          return 1;
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
          return stack.canEquip(equipmentslottype, CuriosContainerV2.this.player);
        }

        @Override
        public boolean mayPickup(@Nonnull Player playerIn) {
          ItemStack itemstack = this.getItem();
          return (itemstack.isEmpty() || playerIn.isCreative() || !EnchantmentHelper
              .hasBindingCurse(itemstack)) && super.mayPickup(playerIn);
        }


        @OnlyIn(Dist.CLIENT)
        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
          return Pair.of(InventoryMenu.BLOCK_ATLAS,
              ARMOR_SLOT_TEXTURES[equipmentslottype.getIndex()]);
        }
      });
    }

    for (int l = 0; l < 3; ++l) {

      for (int j1 = 0; j1 < 9; ++j1) {
        this.addSlot(
            new Slot(player.getInventory(), j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
      }
    }

    for (int i1 = 0; i1 < 9; ++i1) {
      this.addSlot(new Slot(player.getInventory(), i1, 8 + i1 * 18, 142));
    }
    this.addSlot(new Slot(player.getInventory(), 40, 77, 62) {
      @OnlyIn(Dist.CLIENT)
      @Override
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair
            .of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
      }
    });

    if (this.curiosHandler != null) {
      Map<String, ICurioStacksHandler> curioMap = this.curiosHandler.getCurios();
      this.totalPages =
          (int) Math.ceil((double) visibleSlots / maxSlotsPerPage);
      int index = 0;
      int yOffset = 8;

      if (this.totalPages > 1) {
        yOffset += 8;
      }
      int currentColumn = 1;
      int currentRow = 1;
      int slots = 0;
      this.grid.clear();
      this.proxySlots.clear();
      int currentPage = 0;
      int endingIndex = startingIndex + maxSlotsPerPage;

      for (String identifier : curioMap.keySet()) {
        ICurioStacksHandler stacksHandler = curioMap.get(identifier);
        boolean isCosmetic = false;
        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

        if (stacksHandler.hasCosmetic()) {
          this.hasCosmetics = true;

          if (this.isViewingCosmetics) {
            isCosmetic = true;
            stackHandler = stacksHandler.getCosmeticStacks();
          }
        }

        if (stacksHandler.isVisible()) {

          for (int i = 0; i < stackHandler.getSlots(); i++) {

            if (index >= startingIndex && index < endingIndex) {

              if (isCosmetic) {
                this.addSlot(
                    new CurioSlot(this.player, stackHandler, i, identifier,
                        (currentColumn - 1) * 18 + 7 - panelWidth,
                        yOffset + (currentRow - 1) * 18, stacksHandler.getRenders(),
                        stacksHandler.canToggleRendering(), true, true));
              } else {
                this.addSlot(
                    new CurioSlot(this.player, stackHandler, i, identifier,
                        (currentColumn - 1) * 18 + 7 - panelWidth,
                        yOffset + (currentRow - 1) * 18, stacksHandler.getRenders(),
                        stacksHandler.canToggleRendering(), false, false));
              }

              if (this.grid.size() < currentColumn) {
                this.grid.add(1);
              } else {
                this.grid.set(currentColumn - 1, this.grid.get(currentColumn - 1) + 1);
              }

              if (currentColumn == columns) {
                currentColumn = 1;
                currentRow++;
              } else {
                currentColumn++;
              }
            } else {

              if (isCosmetic) {
                this.proxySlots.add(new ProxySlot(currentPage,
                    new CurioSlot(this.player, stackHandler, i, identifier,
                        (currentColumn - 1) * 18 + 7 - panelWidth, yOffset + (currentRow - 1) * 18,
                        stacksHandler.getRenders(), stacksHandler.canToggleRendering(), true,
                        true)));
              } else {
                this.proxySlots.add(new ProxySlot(currentPage,
                    new CurioSlot(this.player, stackHandler, i, identifier,
                        (currentColumn - 1) * 18 + 7 - panelWidth, yOffset + (currentRow - 1) * 18,
                        stacksHandler.getRenders(), stacksHandler.canToggleRendering(), false,
                        false)));
              }
            }
            slots++;

            if (slots >= maxSlotsPerPage) {
              slots = 0;
              currentPage++;
            }
            index++;
          }
        }
      }

      if (!this.isLocalWorld) {
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player),
            new SPacketPage(this.containerId, page));
      }
    }
    this.currentPage = page;
  }

  public void resetSlots() {
    this.setPage(this.currentPage);
  }

  public void toggleCosmetics() {
    this.isViewingCosmetics = !this.isViewingCosmetics;
    this.resetSlots();
  }

  @Override
  public void slotsChanged(@Nonnull Container inventoryIn) {

    if (!this.player.level().isClientSide) {
      ServerPlayer playerMP = (ServerPlayer) this.player;
      ItemStack stack = ItemStack.EMPTY;
      MinecraftServer server = this.player.level().getServer();

      if (server == null) {
        return;
      }
      Optional<CraftingRecipe> recipe = server.getRecipeManager()
          .getRecipeFor(RecipeType.CRAFTING, this.craftMatrix, this.player.level());

      if (recipe.isPresent()) {
        CraftingRecipe craftingRecipe = recipe.get();

        if (this.craftResult.setRecipeUsed(this.player.level(), playerMP, craftingRecipe)) {
          stack = craftingRecipe.assemble(this.craftMatrix, this.player.level().registryAccess());
        }
      }
      this.craftResult.setItem(0, stack);
      this.setRemoteSlot(0, stack);
      playerMP.connection.send(
          new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0,
              stack));
    }
  }

  @Override
  public void removed(@Nonnull Player playerIn) {
    super.removed(playerIn);
    this.craftResult.clearContent();

    if (!playerIn.level().isClientSide) {
      this.clearContainer(playerIn, this.craftMatrix);
    }
  }

  @Override
  public void setItem(int pSlotId, int pStateId, @Nonnull ItemStack pStack) {

    if (this.slots.size() > pSlotId) {
      super.setItem(pSlotId, pStateId, pStack);
    }
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return true;
  }

  @Nonnull
  @Override
  public ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);

    if (slot.hasItem()) {
      ItemStack itemstack1 = slot.getItem();
      itemstack = itemstack1.copy();
      EquipmentSlot entityequipmentslot = Mob.getEquipmentSlotForItem(itemstack);
      if (index == 0) {

        if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
          return ItemStack.EMPTY;
        }
        slot.onQuickCraft(itemstack1, itemstack);
      } else if (index < 5) {

        if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 9) {

        if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
          return ItemStack.EMPTY;
        }
      } else if (entityequipmentslot.getType() == EquipmentSlot.Type.ARMOR
          && !this.slots.get(8 - entityequipmentslot.getIndex()).hasItem()) {
        int i = 8 - entityequipmentslot.getIndex();

        if (!this.moveItemStackTo(itemstack1, i, i + 1, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 46 &&
          !CuriosApi.getItemStackSlots(itemstack, playerIn.level()).isEmpty()) {

        if (!this.moveItemStackTo(itemstack1, 46, this.slots.size(), false)) {
          int page = this.findAvailableSlot(itemstack1);

          if (page != -1) {
            this.moveToPage = page;
            this.moveFromIndex = index;
          } else {
            return ItemStack.EMPTY;
          }
        }
      } else if (entityequipmentslot == EquipmentSlot.OFFHAND && !(this.slots.get(45))
          .hasItem()) {

        if (!this.moveItemStackTo(itemstack1, 45, 46, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 36) {
        if (!this.moveItemStackTo(itemstack1, 36, 45, false)) {
          return ItemStack.EMPTY;
        }
      } else if (index < 45) {
        if (!this.moveItemStackTo(itemstack1, 9, 36, false)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(itemstack1, 9, 45, false)) {
        return ItemStack.EMPTY;
      }

      if (itemstack1.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }

      if (itemstack1.getCount() == itemstack.getCount()) {
        return ItemStack.EMPTY;
      }
      slot.onTake(playerIn, itemstack1);

      if (index == 0) {
        playerIn.drop(itemstack1, false);
      }
    }

    return itemstack;
  }

  protected int findAvailableSlot(ItemStack stack) {
    int result = -1;

    if (stack.isStackable()) {

      for (ProxySlot proxySlot : this.proxySlots) {
        Slot slot = proxySlot.slot();
        ItemStack itemstack = slot.getItem();

        if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
          int j = itemstack.getCount() + stack.getCount();
          int maxSize = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());

          if (j <= maxSize || itemstack.getCount() < maxSize) {
            result = proxySlot.page();
            break;
          }
        }
      }
    }

    if (!stack.isEmpty() && result == -1) {

      for (ProxySlot proxySlot : this.proxySlots) {
        Slot slot1 = proxySlot.slot();
        ItemStack itemstack1 = slot1.getItem();
        if (itemstack1.isEmpty() && slot1.mayPlace(stack)) {
          result = proxySlot.page();
          break;
        }
      }
    }
    return result;
  }

  @Nonnull
  @Override
  public RecipeBookType getRecipeBookType() {
    return RecipeBookType.CRAFTING;
  }

  @Override
  public boolean shouldMoveToInventory(int index) {
    return index != this.getResultSlotIndex();
  }

  @Override
  public void fillCraftSlotsStackedContents(@Nonnull StackedContents itemHelperIn) {
    this.craftMatrix.fillStackedContents(itemHelperIn);
  }

  @Override
  public void clearCraftingContent() {
    this.craftMatrix.clearContent();
    this.craftResult.clearContent();
  }

  @Override
  public boolean recipeMatches(Recipe<? super CraftingContainer> recipeHolder) {
    return recipeHolder.matches(this.craftMatrix, this.player.level());
  }

  @Override
  public int getResultSlotIndex() {
    return 0;
  }

  @Override
  public int getGridWidth() {
    return this.craftMatrix.getWidth();
  }

  @Override
  public int getGridHeight() {
    return this.craftMatrix.getHeight();
  }

  @Override
  public int getSize() {
    return 5;
  }

  public void nextPage() {
    this.setPage(Math.min(this.currentPage + 1, this.totalPages - 1));
  }

  public void prevPage() {
    this.setPage(Math.max(this.currentPage - 1, 0));
  }

  public void checkQuickMove() {

    if (this.moveToPage != -1) {
      this.setPage(this.moveToPage);
      this.quickMoveStack(this.player, this.moveFromIndex);
      this.moveToPage = -1;

      if (!this.isLocalWorld) {
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player),
            new SPacketQuickMove(this.containerId, this.moveFromIndex));
      }
    }
  }

  private record ProxySlot(int page, Slot slot) {

  }
}
