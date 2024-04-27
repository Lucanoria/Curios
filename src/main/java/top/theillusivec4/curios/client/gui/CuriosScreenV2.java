/*
 * Copyright (c) 2018-2023 C4
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

package top.theillusivec4.curios.client.gui;

import static top.theillusivec4.curios.client.gui.CuriosScreen.RECIPE_BUTTON_TEXTURE;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICuriosScreen;
import top.theillusivec4.curios.client.CuriosClientConfig;
import top.theillusivec4.curios.client.CuriosClientConfig.Client;
import top.theillusivec4.curios.client.CuriosClientConfig.Client.ButtonCorner;
import top.theillusivec4.curios.client.KeyRegistry;
import top.theillusivec4.curios.common.inventory.CosmeticCurioSlot;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.inventory.container.CuriosContainerV2;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.client.CPacketPage;
import top.theillusivec4.curios.common.network.client.CPacketToggleRender;

public class CuriosScreenV2 extends EffectRenderingInventoryScreen<CuriosContainerV2>
    implements RecipeUpdateListener, ICuriosScreen {

  static final ResourceLocation CURIO_INVENTORY = new ResourceLocation(CuriosApi.MODID,
      "textures/gui/inventory_revamp.png");

  private final RecipeBookComponent recipeBookGui = new RecipeBookComponent();
  public boolean widthTooNarrow;

  private ImageButton recipeBookButton;
  private CuriosButton buttonCurios;
  private CosmeticButton cosmeticButton;
  private PageButton nextPage;
  private PageButton prevPage;
  private boolean buttonClicked;
  private boolean isRenderButtonHovered;
  public int panelWidth = 0;

  public CuriosScreenV2(CuriosContainerV2 curiosContainer, Inventory playerInventory,
                        Component title) {
    super(curiosContainer, playerInventory, title);
  }

  public static Tuple<Integer, Integer> getButtonOffset(boolean isCreative) {
    Client client = CuriosClientConfig.CLIENT;
    ButtonCorner corner = client.buttonCorner.get();
    int x = 0;
    int y = 0;

    if (isCreative) {
      x += corner.getCreativeXoffset() + client.creativeButtonXOffset.get();
      y += corner.getCreativeYoffset() + client.creativeButtonYOffset.get();
    } else {
      x += corner.getXoffset() + client.buttonXOffset.get();
      y += corner.getYoffset() + client.buttonYOffset.get();
    }
    return new Tuple<>(x, y);
  }

  @Override
  public void init() {

    if (this.minecraft != null) {
      this.panelWidth = this.menu.panelWidth;
      this.leftPos = (this.width - this.imageWidth) / 2;
      this.topPos = (this.height - this.imageHeight) / 2;
      this.widthTooNarrow = true;
      this.recipeBookGui
          .init(this.width, this.height, this.minecraft, true, this.menu);
      this.addWidget(this.recipeBookGui);
      this.setInitialFocus(this.recipeBookGui);

      /*
        This may not be a perfect workaround as it doesn't return the book upon switching back
        to survival mode. Creative inventory doesn't have this problem because it doesn't have
        recipe book at all, but here we only have one Screen and must toggle it circumstantially,
        and sadly Curios' recipe book isn't and must not be an independent object. I can't think
        of better implementation at the moment though.

        Anyhow, this is better than letting the book persist in creative without a way to toggle it.
        @author Extegral
       */
      if (this.getMinecraft().player != null && this.getMinecraft().player.isCreative()
          && this.recipeBookGui.isVisible()) {
        this.recipeBookGui.toggleVisibility();
      }

      Tuple<Integer, Integer> offsets = getButtonOffset(false);
      this.buttonCurios = new CuriosButton(this, this.getGuiLeft() + offsets.getA(),
          this.height / 2 + offsets.getB(), 14, 14, 50, 0, 14, CuriosScreen.CURIO_INVENTORY);

      if (CuriosClientConfig.CLIENT.enableButton.get()) {
        this.addRenderableWidget(this.buttonCurios);
      }

      if (!this.menu.player.isCreative()) {
        this.recipeBookButton =
            new ImageButton(this.leftPos + 104, this.height / 2 - 22, 20, 18, 0, 0, 19,
                RECIPE_BUTTON_TEXTURE, (button) -> {
              this.recipeBookGui.toggleVisibility();
              button.setPosition(this.leftPos + 104, this.height / 2 - 22);
              this.buttonCurios.setPosition(this.leftPos + offsets.getA(),
                  this.height / 2 + offsets.getB());
            });
        this.addRenderableWidget(this.recipeBookButton);
      }
      this.updateRenderButtons();
    }
  }

  public void updateRenderButtons() {
    this.narratables.removeIf(
        widget -> widget instanceof RenderButton || widget instanceof CosmeticButton ||
            widget instanceof PageButton);
    this.children.removeIf(
        widget -> widget instanceof RenderButton || widget instanceof CosmeticButton ||
            widget instanceof PageButton);
    this.renderables.removeIf(
        widget -> widget instanceof RenderButton || widget instanceof CosmeticButton ||
            widget instanceof PageButton);
    this.panelWidth = this.menu.panelWidth;

    if (this.menu.hasCosmetics) {
      this.cosmeticButton =
          new CosmeticButton(this, this.getGuiLeft() + 17, this.getGuiTop() - 18, 20, 17);
      this.addRenderableWidget(this.cosmeticButton);
    }

    if (this.menu.totalPages > 1) {
      this.nextPage = new PageButton(this, this.getGuiLeft() + 17, this.getGuiTop() + 2, 11, 12,
          PageButton.Type.NEXT);
      this.addRenderableWidget(this.nextPage);
      this.prevPage = new PageButton(this, this.getGuiLeft() + 17, this.getGuiTop() + 2, 11, 12,
          PageButton.Type.PREVIOUS);
      this.addRenderableWidget(this.prevPage);
    }

    for (Slot inventorySlot : this.menu.slots) {

      if (inventorySlot instanceof CurioSlot curioSlot &&
          !(inventorySlot instanceof CosmeticCurioSlot)) {

        if (curioSlot.canToggleRender()) {
          this.addRenderableWidget(new RenderButton(curioSlot, this.leftPos + inventorySlot.x + 11,
              this.topPos + inventorySlot.y - 3, 8, 8, 75, 0, 8, CURIO_INVENTORY,
              (button) -> NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
                  new CPacketToggleRender(curioSlot.getIdentifier(),
                      inventorySlot.getSlotIndex()))));
        }
      }
    }
  }

  @Override
  public void containerTick() {
    super.containerTick();
    this.recipeBookGui.tick();
  }

  @Override
  public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY,
                     float partialTicks) {
    this.renderBackground(guiGraphics);

    if (this.recipeBookGui.isVisible() && this.widthTooNarrow) {
      this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
      this.recipeBookGui.render(guiGraphics, mouseX, mouseY, partialTicks);
    } else {
      this.recipeBookGui.render(guiGraphics, mouseX, mouseY, partialTicks);
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      this.recipeBookGui
          .renderGhostRecipe(guiGraphics, this.leftPos, this.topPos, true, partialTicks);

      boolean isButtonHovered = false;

      for (Renderable button : this.renderables) {

        if (button instanceof RenderButton) {
          ((RenderButton) button).renderButtonOverlay(guiGraphics, mouseX, mouseY, partialTicks);

          if (((RenderButton) button).isHovered()) {
            isButtonHovered = true;
          }
        }
      }
      this.isRenderButtonHovered = isButtonHovered;
      LocalPlayer clientPlayer = Minecraft.getInstance().player;

      if (!this.isRenderButtonHovered && clientPlayer != null && clientPlayer.inventoryMenu
          .getCarried().isEmpty() && this.getSlotUnderMouse() != null) {
        Slot slot = this.getSlotUnderMouse();

        if (slot instanceof CurioSlot slotCurio && !slot.hasItem()) {
          guiGraphics.renderTooltip(this.font, Component.literal(slotCurio.getSlotName()), mouseX,
              mouseY);
        }
      }
    }
    this.renderTooltip(guiGraphics, mouseX, mouseY);
  }

  @Override
  protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    Minecraft mc = this.minecraft;

    if (mc != null) {
      LocalPlayer clientPlayer = mc.player;

      if (clientPlayer != null && clientPlayer.inventoryMenu.getCarried().isEmpty()) {

        if (this.isRenderButtonHovered) {
          guiGraphics.renderTooltip(this.font, Component.translatable("gui.curios.toggle"), mouseX,
              mouseY);
        } else if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
          guiGraphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
        }
      }
    }
  }

  @Override
  public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {

    if (this.recipeBookGui.isVisible() && this.widthTooNarrow) {
      this.recipeBookGui.toggleVisibility();
      return true;
    } else if (KeyRegistry.openCurios
        .isActiveAndMatches(InputConstants.getKey(p_keyPressed_1_, p_keyPressed_2_))) {
      LocalPlayer playerEntity = this.getMinecraft().player;

      if (playerEntity != null) {
        playerEntity.closeContainer();
      }
      return true;
    } else {
      return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }
  }

  @Override
  protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {

    if (this.minecraft != null && this.minecraft.player != null) {
      guiGraphics.drawString(this.font, this.title, 97, 6, 4210752, false);
    }
  }

  /**
   * Draws the background layer of this container (behind the item).
   */

  @Override
  protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTicks, int mouseX,
                          int mouseY) {

    if (this.minecraft != null && this.minecraft.player != null) {

      if (scrollCooldown > 0 && this.minecraft.player.tickCount % 5 == 0) {
        scrollCooldown--;
      }
      this.panelWidth = this.menu.panelWidth;
      int i = this.leftPos;
      int j = this.topPos;
      guiGraphics.blit(INVENTORY_LOCATION, i, j, 0, 0, 176, this.imageHeight);
      InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, i + 51, j + 75, 30,
          (float) (i + 51) - mouseX, (float) (j + 75 - 50) - mouseY, this.minecraft.player);
      CuriosApi.getCuriosInventory(this.minecraft.player).ifPresent(handler -> {
        int xOffset = -33;
        int yOffset = j;
        boolean pageOffset = this.menu.totalPages > 1;

        if (this.menu.hasCosmetics) {
          guiGraphics.blit(CURIO_INVENTORY, i + xOffset + 2, yOffset - 23, 32, 0, 28, 24);
        }
        List<Integer> grid = this.menu.grid;
        xOffset -= (grid.size() - 1) * 18;

        // render backplate
        for (int r = 0; r < grid.size(); r++) {
          int rows = grid.get(0);
          int upperHeight = 7 + rows * 18;
          int xTexOffset = 91;

          if (pageOffset) {
            upperHeight += 8;
          }

          if (r != 0) {
            xTexOffset += 7;
          }
          guiGraphics.blit(CURIO_INVENTORY, i + xOffset, yOffset, xTexOffset, 0, 25,
              upperHeight);
          guiGraphics.blit(CURIO_INVENTORY, i + xOffset, yOffset + upperHeight, xTexOffset, 159, 25,
              7);

          if (grid.size() == 1) {
            xTexOffset += 7;
            guiGraphics.blit(CURIO_INVENTORY, i + xOffset + 7, yOffset, xTexOffset, 0, 25,
                upperHeight);
            guiGraphics.blit(CURIO_INVENTORY, i + xOffset + 7, yOffset + upperHeight, xTexOffset,
                159, 25, 7);
          }

          if (r == 0) {
            xOffset += 25;
          } else {
            xOffset += 18;
          }
        }
        xOffset -= (grid.size()) * 18;

        if (pageOffset) {
          yOffset += 8;
        }

        // render slots
        for (int rows : grid) {
          int upperHeight = rows * 18;

          guiGraphics.blit(CURIO_INVENTORY, i + xOffset, yOffset + 7, 7, 7, 18, upperHeight);
          xOffset += 18;
        }
        RenderSystem.enableBlend();

        for (Slot slot : this.menu.slots) {

          if (slot instanceof CurioSlot curioSlot && curioSlot.isCosmetic()) {
            guiGraphics.blit(CURIO_INVENTORY, slot.x + this.getGuiLeft() - 1,
                slot.y + this.getGuiTop() - 1, 32, 50, 18, 18);
          }
        }
        RenderSystem.disableBlend();
      });
    }
  }

  /**
   * Test if the 2D point is in a rectangle (relative to the GUI). Args : rectX, rectY, rectWidth,
   * rectHeight, pointX, pointY
   */
  @Override
  protected boolean isHovering(int rectX, int rectY, int rectWidth, int rectHeight,
                               double pointX, double pointY) {

    if (this.isRenderButtonHovered) {
      return false;
    }
    return (!this.widthTooNarrow || !this.recipeBookGui.isVisible()) && super
        .isHovering(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
  }

  /**
   * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
   */
  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

    if (this.recipeBookGui.mouseClicked(mouseX, mouseY, mouseButton)) {
      return true;
    }
    return this.widthTooNarrow && this.recipeBookGui.isVisible() || super
        .mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public boolean mouseReleased(double mouseReleased1, double mouseReleased3, int mouseReleased5) {

    if (this.buttonClicked) {
      this.buttonClicked = false;
      return true;
    } else {
      return super.mouseReleased(mouseReleased1, mouseReleased3, mouseReleased5);
    }
  }

  private static int scrollCooldown = 0;

  @Override
  public boolean mouseScrolled(double pMouseScrolled1, double pMouseScrolled3,
                               double pMouseScrolled5) {

    if (this.menu.totalPages > 1 && pMouseScrolled1 < this.getGuiLeft() &&
        pMouseScrolled1 > this.getGuiLeft() - this.panelWidth &&
        pMouseScrolled3 > this.getGuiTop() &&
        pMouseScrolled3 < this.getGuiTop() + this.imageHeight && scrollCooldown <= 0) {
      NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(),
          new CPacketPage(this.getMenu().containerId, pMouseScrolled5 < 0));
      scrollCooldown = 2;
    }
    return super.mouseScrolled(pMouseScrolled1, pMouseScrolled3, pMouseScrolled5);
  }

  @Override
  protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn,
                                      int mouseButton) {
    boolean flag = mouseX < guiLeftIn || mouseY < guiTopIn || mouseX >= guiLeftIn + this.imageWidth
        || mouseY >= guiTopIn + this.imageHeight;
    return this.recipeBookGui.hasClickedOutside(mouseX, mouseY, this.leftPos, this.topPos,
        this.imageWidth, this.imageHeight, mouseButton) && flag;
  }

  @Override
  protected void slotClicked(@Nonnull Slot slotIn, int slotId, int mouseButton,
                             @Nonnull ClickType type) {
    super.slotClicked(slotIn, slotId, mouseButton, type);
    this.recipeBookGui.slotClicked(slotIn);
  }

  @Override
  public void recipesUpdated() {
    this.recipeBookGui.recipesUpdated();
  }

  @Nonnull
  @Override
  public RecipeBookComponent getRecipeBookComponent() {
    return this.recipeBookGui;
  }
}
