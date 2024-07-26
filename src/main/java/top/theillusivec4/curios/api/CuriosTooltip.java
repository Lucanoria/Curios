package top.theillusivec4.curios.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.ISlotType;

public class CuriosTooltip {

  private final List<Component> content = new ArrayList<>();
  private final Set<String> identifiers = new HashSet<>();
  private ItemStack stack = ItemStack.EMPTY;
  private LivingEntity livingEntity;

  public CuriosTooltip append(Component component) {
    this.content.add(component);
    return this;
  }

  public CuriosTooltip appendHeader(MutableComponent component) {
    return this.append(component.withStyle(ChatFormatting.GOLD));
  }

  public CuriosTooltip appendSlotHeader(String identifier) {
    return this.append(
        Component.translatable("curios.modifiers." + identifier).withStyle(ChatFormatting.GOLD));
  }

  public CuriosTooltip appendAdditive(MutableComponent component) {
    return this.append(component.withStyle(ChatFormatting.BLUE));
  }

  public CuriosTooltip appendSubtractive(MutableComponent component) {
    return this.append(component.withStyle(ChatFormatting.RED));
  }

  public CuriosTooltip appendEqual(MutableComponent component) {
    return this.append(component.withStyle(ChatFormatting.DARK_GREEN));
  }

  public CuriosTooltip forSlots(String... identifiers) {
    this.identifiers.addAll(Arrays.asList(identifiers));
    return this;
  }

  public CuriosTooltip forSlots(ItemStack stack) {
    this.stack = stack;
    return this;
  }

  public CuriosTooltip forSlots(ItemStack stack, LivingEntity livingEntity) {
    this.stack = stack;
    this.livingEntity = livingEntity;
    return this;
  }

  public List<Component> build() {
    List<Component> result = new ArrayList<>();
    Set<String> ids = new TreeSet<>();

    if (!this.identifiers.isEmpty()) {
      ids.addAll(this.identifiers);
    } else if (!this.stack.isEmpty()) {
      Map<String, ISlotType> map =
          this.livingEntity != null ? CuriosApi.getItemStackSlots(stack, this.livingEntity) :
              CuriosApi.getItemStackSlots(stack, true);
      ids.addAll(map.keySet());
    }

    for (String identifier : ids) {
      result.add(Component.empty());
      result.add(Component.translatable("curios.modifiers." + identifier)
          .withStyle(ChatFormatting.GOLD));
      result.addAll(this.content);
    }
    return result;
  }
}
