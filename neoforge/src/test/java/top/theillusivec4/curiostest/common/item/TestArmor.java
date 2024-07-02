package top.theillusivec4.curiostest.common.item;

import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;

public class TestArmor extends ArmorItem {

  private static final UUID ARMOR_UUID = UUID.fromString("26f348df-ffb8-48cc-9664-310ac8e2e1cf");

  public TestArmor(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
    super(pMaterial, pType, pProperties);
  }

  @Override
  public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
    ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers(stack);
    modifiers = CuriosApi.withSlotModifier(modifiers, "ring", ARMOR_UUID, 1,
        AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.bySlot(this.type.getSlot()));
    modifiers = CuriosApi.withSlotModifier(modifiers, "necklace", ARMOR_UUID, -3,
        AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.bySlot(this.type.getSlot()));
    return modifiers;
  }
}
